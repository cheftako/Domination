package controllers;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/3/13
 * Time: 9:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class Secured extends Security.Authenticator {

    @Override
    public String getUsername(Context ctx) {
        System.out.println("Got a contact of " + ctx + " with an admin username of " + ctx.session().get("adminUsername"));
        String username = ctx.session().get("adminUsername");
        if (username != null)
        {
            return username;
        }
        System.out.println("getUsername is " + ctx.session().get("username"));
        return ctx.session().get("username");
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        return redirect(routes.Application.login());
    }
}
