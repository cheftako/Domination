package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/18/13
 * Time: 10:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class Game extends Model {
    @Id
    public Long id;

    public Long first;

    @Constraints.Required
    public Long second;

    @Constraints.Required
    public Long third;

    public String jsonFile;
}
