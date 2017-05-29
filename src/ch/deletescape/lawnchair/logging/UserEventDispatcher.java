/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.deletescape.lawnchair.logging;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import java.util.Locale;

import ch.deletescape.lawnchair.DropTarget;
import ch.deletescape.lawnchair.ItemInfo;
import ch.deletescape.lawnchair.userevent.nano.LauncherLogProto.Action;
import ch.deletescape.lawnchair.userevent.nano.LauncherLogProto.LauncherEvent;
import ch.deletescape.lawnchair.userevent.nano.LauncherLogProto.Target;

/**
 * Manages the creation of {@link LauncherEvent}.
 * To debug this class, execute following command before sideloading a new apk.
 * <p>
 * $ adb shell setprop log.tag.UserEvent VERBOSE
 */
public class UserEventDispatcher {

    private final static int MAXIMUM_VIEW_HIERARCHY_LEVEL = 5;

    private final boolean mIsVerbose;

    /**
     * Implemented by containers to provide a launch source for a given child.
     */
    public interface LogContainerProvider {

        /**
         * Copies data from the source to the destination proto.
         *
         * @param v            source of the data
         * @param info         source of the data
         * @param target       dest of the data
         * @param targetParent dest of the data
         */
        void fillInLogContainerData(View v, ItemInfo info, Target target, Target targetParent);
    }

    /**
     * Recursively finds the parent of the given child which implements IconLogInfoProvider
     */
    public static LogContainerProvider getLaunchProviderRecursive(View v) {
        ViewParent parent;

        if (v != null) {
            parent = v.getParent();
        } else {
            return null;
        }

        // Optimization to only check up to 5 parents.
        int count = MAXIMUM_VIEW_HIERARCHY_LEVEL;
        while (parent != null && count-- > 0) {
            if (parent instanceof LogContainerProvider) {
                return (LogContainerProvider) parent;
            } else {
                parent = parent.getParent();
            }
        }
        return null;
    }

    private String TAG = "UserEvent";

    private long mElapsedContainerMillis;
    private long mElapsedSessionMillis;
    private long mActionDurationMillis;

    public UserEventDispatcher() {
        mIsVerbose = false;
    }

    //                      APP_ICON    SHORTCUT    WIDGET
    // --------------------------------------------------------------
    // packageNameHash      required    optional    required
    // componentNameHash    required                required
    // intentHash                       required
    // --------------------------------------------------------------

    protected LauncherEvent createLauncherEvent(View v, Intent intent) {
        LauncherEvent event = LoggerUtils.initLauncherEvent(
                Action.TOUCH, v, Target.CONTAINER);
        event.action.touch = Action.TAP;

        // Fill in grid(x,y), pageIndex of the child and container type of the parent
        // TODO: make this percolate up the view hierarchy if needed.
        int idx = 0;
        LogContainerProvider provider = getLaunchProviderRecursive(v);
        if (v == null || !(v.getTag() instanceof ItemInfo) || provider == null) {
            return null;
        }
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        provider.fillInLogContainerData(v, itemInfo, event.srcTarget[idx], event.srcTarget[idx + 1]);

        event.srcTarget[idx].intentHash = intent.hashCode();
        ComponentName cn = intent.getComponent();
        if (cn != null) {
            event.srcTarget[idx].packageNameHash = cn.getPackageName().hashCode();
            event.srcTarget[idx].componentHash = cn.hashCode();
        }
        return event;
    }

    public void logAppLaunch(View v, Intent intent) {
        LauncherEvent ev = createLauncherEvent(v, intent);
        if (ev == null) {
            return;
        }
        dispatchUserEvent(ev);
    }

    public void logActionOnControl(int action, int controlType) {
        LauncherEvent event = LoggerUtils.initLauncherEvent(Action.TOUCH, Target.CONTROL);
        event.action.touch = action;
        event.srcTarget[0].controlType = controlType;
        dispatchUserEvent(event);
    }

    public void logActionOnContainer(int action, int dir, int containerType) {
        LauncherEvent event = LoggerUtils.initLauncherEvent(Action.TOUCH, Target.CONTAINER);
        event.action.touch = action;
        event.action.dir = dir;
        event.srcTarget[0].containerType = containerType;
        dispatchUserEvent(event);
    }

    public void logDeepShortcutsOpen(View icon) {
        LauncherEvent event = LoggerUtils.initLauncherEvent(
                Action.TOUCH, icon, Target.CONTAINER);
        LogContainerProvider provider = getLaunchProviderRecursive(icon);
        if (icon == null || !(icon.getTag() instanceof ItemInfo)) {
            return;
        }
        ItemInfo info = (ItemInfo) icon.getTag();
        provider.fillInLogContainerData(icon, info, event.srcTarget[0], event.srcTarget[1]);
        event.action.touch = Action.LONGPRESS;
        dispatchUserEvent(event);
    }

    public void logDragNDrop(DropTarget.DragObject dragObj, View dropTargetAsView) {
        LauncherEvent event = LoggerUtils.initLauncherEvent(Action.TOUCH,
                dragObj.originalDragInfo,
                Target.CONTAINER,
                dropTargetAsView);
        event.action.touch = Action.DRAGDROP;

        dragObj.dragSource.fillInLogContainerData(null, dragObj.originalDragInfo,
                event.srcTarget[0], event.srcTarget[1]);

        if (dropTargetAsView instanceof LogContainerProvider) {
            ((LogContainerProvider) dropTargetAsView).fillInLogContainerData(null,
                    dragObj.dragInfo, event.destTarget[0], event.destTarget[1]);

        }
        event.actionDurationMillis = SystemClock.uptimeMillis() - mActionDurationMillis;
        dispatchUserEvent(event);
    }

    /**
     * Currently logs following containers: workspace, allapps, widget tray.
     */
    public final void resetElapsedContainerMillis() {
        mElapsedContainerMillis = SystemClock.uptimeMillis();
    }

    public final void resetElapsedSessionMillis() {
        mElapsedSessionMillis = SystemClock.uptimeMillis();
        mElapsedContainerMillis = SystemClock.uptimeMillis();
    }

    public final void resetActionDurationMillis() {
        mActionDurationMillis = SystemClock.uptimeMillis();
    }

    public void dispatchUserEvent(LauncherEvent ev) {
        ev.elapsedContainerMillis = SystemClock.uptimeMillis() - mElapsedContainerMillis;
        ev.elapsedSessionMillis = SystemClock.uptimeMillis() - mElapsedSessionMillis;

        if (!mIsVerbose) {
            return;
        }
        Log.d(TAG, String.format(Locale.US,
                "\naction:%s\n Source child:%s\tparent:%s",
                LoggerUtils.getActionStr(ev.action),
                LoggerUtils.getTargetStr(ev.srcTarget != null ? ev.srcTarget[0] : null),
                LoggerUtils.getTargetStr(ev.srcTarget != null && ev.srcTarget.length > 1 ?
                        ev.srcTarget[1] : null)));
        if (ev.destTarget != null && ev.destTarget.length > 0) {
            Log.d(TAG, String.format(Locale.US,
                    " Destination child:%s\tparent:%s",
                    LoggerUtils.getTargetStr(ev.destTarget[0]),
                    LoggerUtils.getTargetStr(ev.destTarget.length > 1 ?
                            ev.destTarget[1] : null)));
        }
        Log.d(TAG, String.format(Locale.US,
                " Elapsed container %d ms session %d ms action %d ms",
                ev.elapsedContainerMillis,
                ev.elapsedSessionMillis,
                ev.actionDurationMillis));
    }
}
