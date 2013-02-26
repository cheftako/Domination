package models;

import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.ObjectId;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.modules.mongodb.jackson.MongoDB;

import javax.persistence.Id;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/18/13
 * Time: 10:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class Game /* extends Model */ {
    @Id
    @ObjectId
    public String id;

    public String first;

    @Constraints.Required
    public String second;

    @Constraints.Required
    public String third;

    public String jsonFile;

    private static JacksonDBCollection<Game, String> games = MongoDB.getCollection("games", Game.class, String.class);

    public void save() {
        games.save(this);
    }

    public static List<Game> all()
    {
        //return find.all();
        return games.find().toArray();
    }
}
