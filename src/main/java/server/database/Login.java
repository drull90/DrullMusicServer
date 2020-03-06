package server.database;

public class Login {

    DbAccess db;

    public Login(DbAccess db) {
        this.db = db;
    }

    public  boolean validate(String user, String pass) {

        return false;
    }


}