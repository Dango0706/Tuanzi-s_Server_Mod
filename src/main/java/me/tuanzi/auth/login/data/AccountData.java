package me.tuanzi.auth.login.data;

import java.util.ArrayList;
import java.util.List;

public class AccountData {
    private List<PlayerAccount> accounts;

    public AccountData() {
        this.accounts = new ArrayList<>();
    }

    public List<PlayerAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<PlayerAccount> accounts) {
        this.accounts = accounts;
    }
}
