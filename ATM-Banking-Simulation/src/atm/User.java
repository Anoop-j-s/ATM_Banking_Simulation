/*
 * User.java - Session wrapper for the authenticated principal.
 */
package atm;

public class User {
    private final Account account;

    public User(Account account) {
        this.account = account;
    }

    public Account getAccount() { return account; }
    public boolean isAdmin() { return account.getRole() == Account.Role.ADMIN; }
    public String getDisplayName() { return account.getName(); }
}
