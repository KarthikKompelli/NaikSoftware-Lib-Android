package ua.naiksoftware.android.adapter.actionhandler;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ua.naiksoftware.android.adapter.actionhandler.action.Action;
import ua.naiksoftware.android.adapter.actionhandler.action.BaseAction;
import ua.naiksoftware.android.adapter.actionhandler.action.Cancelable;
import ua.naiksoftware.android.adapter.actionhandler.listener.ActionClickListener;
import ua.naiksoftware.android.adapter.actionhandler.listener.ActionInterceptor;
import ua.naiksoftware.android.adapter.actionhandler.listener.OnActionFiredListener;

/**
 * Use ActionHandler to manage action and bind them to view
 */
public class ActionHandler implements ActionClickListener {

    // Actions which was added to the handler
    protected final List<ActionPair> mActions;

    // Callback to be invoked when an action is executed successfully
    protected OnActionFiredListener mOnActionFiredListener;

    // Callback to be invoked after a view with an action is clicked and before action handling started.
    // Can intercept an action to prevent it to be fired
    private ActionInterceptor mActionInterceptor;

    /**
     * @param actions list of actions to handle by this handler
     */
    protected ActionHandler(List<ActionPair> actions) {
        mActions = actions != null ? actions : Collections.<ActionPair>emptyList();
    }

    /**
     * Set new callback to be invoked when an action is executed successfully
     * Note: It is called only for BaseActions.
     * You should call {@link BaseAction#notifyOnActionFired(View, String, Object)} to invoke this callback.
     *
     * @param actionFiredListener new callback to be invoked when an action is executed successfully
     */
    public void setOnActionFiredListener(OnActionFiredListener actionFiredListener) {
        OnActionFiredListener oldListener = null;
        if (mOnActionFiredListener != null) oldListener = mOnActionFiredListener;
        mOnActionFiredListener = actionFiredListener;
        for (ActionPair actionPair : mActions) {
            if (actionPair.action instanceof BaseAction) {
                BaseAction baseAction = ((BaseAction) actionPair.action);
                baseAction.removeActionFireListener(oldListener);
                baseAction.addActionFiredListener(mOnActionFiredListener);
            }
        }
    }

    /**
     * Set new callback to be invoked after a view with an action is clicked and before action handling started.
     * Can intercept an action to prevent it to be fired
     *
     * @param actionInterceptor The interceptor, which can prevent actions to be fired
     */
    public void setActionInterceptor(ActionInterceptor actionInterceptor) {
        mActionInterceptor = actionInterceptor;
    }

    /**
     * Check if there is at least one action that can try to handle {@code actionType}
     *
     * @param actionType The action type to check
     * @return true if there is at least one action that can try to handle {@code actionType},
     * false otherwise.
     */
    public boolean canHandle(final String actionType) {
        for (ActionPair actionPair : mActions) {
            if (equals(actionType, actionPair.actionType)) return true;
        }
        return false;
    }

    /**
     * Called when a view with an action is clicked.
     *
     * @param view       The view that was clicked.
     * @param actionType The action type, which appointed to the view
     * @param model      The model, which  appointed to the view and should be handled
     */
    @Override
    public void onActionClick(View view, String actionType, Object model) {
        if (view == null) return;
        final Context context = view.getContext();

        fireAction(context, view, actionType, model);

    }

    /**
     * Call for initiate actions to fire.
     *
     * @param context    The Context, which generally get from view by {@link View#getContext()}
     * @param view       The view that was clicked.
     * @param actionType The action type, which appointed to the view
     * @param model      The model, which  appointed to the view and should be handled
     */
    public void fireAction(Context context, View view, String actionType, Object model) {
        if (mActionInterceptor != null
                && mActionInterceptor.onInterceptAction(context, view, actionType, model)) return;

        for (ActionPair actionPair : mActions) {
            if (actionPair.actionType == null || actionPair.actionType.equals(actionType)) {
                final Action action = actionPair.action;
                if (action != null && action.isModelAccepted(model)) {
                    //noinspection unchecked
                    action.onFireAction(context, view, actionType, model);
                }
            }
        }
    }

    /**
     * Call this method to force actions to cancel.
     * Usually, you may need to call this on Activity destroy to free resources which
     * can lead to memory leak and stop pending transaction or async calls.
     */
    public final void cancelAll() {
        for (ActionPair actionPair : mActions) {
            if (actionPair.action instanceof Cancelable) {
                ((Cancelable) actionPair.action).cancel();
            }
        }
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}.
     */
    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    /**
     * The Builder for configure action handler
     */
    public static final class Builder {
        private List<ActionPair> mActions;
        private OnActionFiredListener mActionFiredListener;
        private ActionInterceptor mActionInterceptor;

        public Builder() {
            mActions = new ArrayList<>();
        }

        /**
         * Add an action to the action handler
         *
         * @param actionType The type of action
         * @param action     The action
         * @return
         */
        public Builder addAction(String actionType, Action action) {
            mActions.add(new ActionPair(actionType, action));
            return this;
        }

        /**
         * Set new callback to be invoked when an action is executed successfully
         * Note: It is called only for BaseActions.
         * You should call {@link BaseAction#notifyOnActionFired(View, String, Object)} to invoke this callback.
         *
         * @param actionFiredListener new callback to be invoked when an action is executed successfully
         */
        public Builder setActionFiredListener(final OnActionFiredListener actionFiredListener) {
            mActionFiredListener = actionFiredListener;
            return this;
        }

        /**
         * Set callback to be invoked after a view with an action is clicked and before action handling started.
         * Can intercept an action to prevent it to be fired
         *
         * @param actionInterceptor The interceptor, which can prevent actions to be fired
         */
        public Builder setActionInterceptor(ActionInterceptor actionInterceptor) {
            mActionInterceptor = actionInterceptor;
            return this;
        }

        public ActionHandler build() {
            final ActionHandler actionHandler = new ActionHandler(mActions);
            if (mActionFiredListener != null) {
                actionHandler.setOnActionFiredListener(mActionFiredListener);
            }
            if (mActionInterceptor != null) {
                actionHandler.setActionInterceptor(mActionInterceptor);
            }
            return actionHandler;
        }
    }

    /**
     * Holder for an action and corresponded type
     */
    public static class ActionPair {
        public String actionType;
        public Action action;

        public ActionPair(String actionType, Action action) {
            this.actionType = actionType;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ActionPair that = (ActionPair) o;

            if (actionType != null ? !actionType.equals(that.actionType) : that.actionType != null)
                return false;
            return action != null ? action.equals(that.action) : that.action == null;

        }

        @Override
        public int hashCode() {
            int result = actionType != null ? actionType.hashCode() : 0;
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ActionPair{" +
                    "actionType='" + actionType + '\'' +
                    ", action=" + action +
                    '}';
        }
    }
}
