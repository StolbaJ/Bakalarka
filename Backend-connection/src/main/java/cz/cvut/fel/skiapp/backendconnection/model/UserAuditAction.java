package cz.cvut.fel.skiapp.backendconnection.model;

public enum UserAuditAction {
    /** Vytvoření nového uživatele */
    CREATE,
    /** Změna role/oprávnění */
    UPDATE_ROLE,
    /** Reset hesla */
    RESET_PASSWORD,
    /** Deaktivace účtu */
    DEACTIVATE,
    /** Reaktivace účtu */
    REACTIVATE,
    /** Změna vlastního hesla */
    CHANGE_OWN_PASSWORD
}
