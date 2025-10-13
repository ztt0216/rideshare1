package com.unimelb.rideshare.presentation;

/**
 * Menu options exposed to console users.
 */
public enum MenuOption {
    REGISTER_DRIVER(1, "Register driver"),
    UPDATE_DRIVER_AVAILABILITY(2, "Update driver availability"),
    REGISTER_RIDER(3, "Register rider"),
    REQUEST_RIDE(4, "Create ride request"),
    MATCH_RIDE(5, "Match ride request"),
    ACCEPT_RIDE(6, "Accept ride"),
    START_RIDE(7, "Start ride"),
    COMPLETE_RIDE(8, "Complete ride"),
    CANCEL_REQUEST(9, "Cancel ride request"),
    LIST_DRIVERS(10, "List drivers"),
    LIST_OPEN_REQUESTS(11, "List open requests"),
    LIST_ACTIVE_RIDES(12, "List active rides"),
    EXIT(0, "Exit");

    private final int code;
    private final String label;

    MenuOption(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static MenuOption fromCode(int code) {
        for (MenuOption option : values()) {
            if (option.code == code) {
                return option;
            }
        }
        throw new IllegalArgumentException("Unknown menu option: " + code);
    }
}
