package dev.kingtux.tms.api;

/**
 * A KeyMapping may implement this to be handled before the current screen/vanilla input
 * handling sees the key. Return true from either callback to swallow the event.
 */
public interface PriorityKeyBinding {
    default boolean onPressedPriority() { return false; }

    default boolean onReleasedPriority() { return false; }
}
