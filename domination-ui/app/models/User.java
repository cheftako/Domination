package models;

import net.vz.mongodb.jackson.*;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import play.modules.mongodb.jackson.MongoDB;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.Entity;
//import javax.persistence.Id;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/3/13
 * Time: 5:34 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class User /*extends Model*/ {
    @Id
    @ObjectId
    public String id;

    @Required
    public String name;

    @Required
    public String password;

    public String team;

    public String description;

    public Integer jarVersion;

    /*public static Finder<Long, User> find = new Finder(
        Long.class, User.class
    );*/

    private static JacksonDBCollection<User, String> users = MongoDB.getCollection("users", User.class, String.class);

    public static List<User> all()
    {
        //return find.all();
        return users.find().toArray();
    }

    public static void create(User user)
    {
        user.jarVersion = 0;
        //user.save();
        WriteResult result = users.insert(user);
        System.out.println("***** Result is " + result.toString());
        System.out.println("***** Error is " + result.getError());
        System.out.println("***** Last Error is " + result.getLastError());
        System.out.println("***** Saved Id is " + result.getSavedId());
    }

    public static void delete(String id)
    {
        // find.ref(id).delete();
        User user = users.findOneById(id);
        if (user != null)
        {
            users.remove(user);
        }
    }

    public static User getUserByName(String username)
    {
        System.out.println("Trying to get a new user with name \"" + username + "\"");
        User result = null;
        DBCursor<User> cursor = users.find(DBQuery.all("name", username));
        if (cursor.hasNext()) {
            result = cursor.next();
        }
        return result;
    }

    public static User getUserById(String id) {
        //return find.byId(id);
        return users.findOneById(id);
    }

    public static List<User> getUsers()
    {
        //return find.all();
        return users.find().toArray();
    }

    public static List<User> getEligibleUsers()
    {
        //return find.where().ge("jarVersion", 1).findList();
        return users.find().greaterThanEquals("jarVersion", 1).toArray();
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

    public void save() {
        users.save(this);
    }
}
