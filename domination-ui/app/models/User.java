package models;

import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/3/13
 * Time: 5:34 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class User extends Model {
    @Id
    public Long id;

    @Required
    public String name;

    @Required
    public String password;

    public String team;

    public String description;

    public Integer jarVersion;

    public static Finder<Long, User> find = new Finder(
        Long.class, User.class
    );

    public static List<User> all() {
        return find.all();
    }

    public static void create(User user) {
        user.jarVersion = 0;
        user.save();
    }

    public static void delete(Long id) {
        find.ref(id).delete();
    }

    public static User getUserByName(String username)
    {
        return find.where().eq("name", username).findUnique();
    }

    public static User getUserById(Long id) {
        return find.byId(id);
    }

    public static List<User> getUsers()
    {
        return find.all();
    }

    public static List<User> getEligibleUsers()
    {
        return find.where().ge("jarVersion", 1).findList();
    }

    public static boolean authenticate(String name, String password)
    {
        User user = getUserByName(name);
        if (user == null)
        {
            return false;
        }
        return user.password.equals(password);
    }

    public String validate() {
        if (getUserByName(name) != null)
        {
            System.out.println("User name " + name + " is already taken.");
            return "User name is already taken";
        }
        return null;
    }
}
