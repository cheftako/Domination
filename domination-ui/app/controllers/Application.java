package controllers;

import models.User;
import play.api.libs.MimeTypes;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.ResponseHeader;
import play.api.mvc.SimpleResult;
import play.data.Form;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import scala.collection.immutable.Map;
import scalax.io.support.FileUtils;

import java.io.*;
import java.sql.Date;

public class Application extends Controller {

    static Form<User> userForm = form(User.class);

    private static final String IMAGE_FILE_LOCATION = "storage/images";
  
    public static Result index() {
        return ok("Hello world.");
    }

    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.login.render(loginForm));
        }
        else {
            System.out.println("LoginForm = " + loginForm);
            session().clear();
            session("username", loginForm.get().username);
            return redirect(routes.Application.userMain());
        }
    }

    public static Result login() {
        return ok(
                views.html.login.render(form(Login.class))
        );
    }

    public static Result adminAuthenticate() {
        Form<AdminLogin> loginForm = form(AdminLogin.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.adminLogin.render(loginForm));
        }
        else {
            session().clear();
            session("adminUsername", loginForm.get().username);
            return redirect(routes.Application.users());
        }
    }

    public static Result adminLogin() {
        return ok(
                views.html.adminLogin.render(form(AdminLogin.class))
        );
    }

    public static Result logout() {
        session().clear();
        return ok(
                views.html.login.render(form(Login.class))
        );
    }

    public static Result signup() {
        return ok(
            views.html.signup.render(userForm)
        );
    }

    public static Result newUser() {
        Form<User> filledForm = userForm.bindFromRequest();
        if (filledForm.hasErrors()) {
            return badRequest(
                    views.html.signup.render(filledForm)
            );
        }
        else {
            User.create(filledForm.get());
            session().clear();
            System.out.println("Setting the session username of " + filledForm.get().name);
            session("username", filledForm.get().name);
            return redirect(routes.Application.userMain());
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result userMain() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        System.out.println("Got a user object of " + user + " when using the name " + username);
        return ok(
                views.html.userMain.render(user)
        );
    }

    @Security.Authenticated(Secured.class)
    public static Result users() {
        return ok(
            views.html.users.render(User.all(), userForm)
        );
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadImagePage() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        return ok(views.html.uploadImage.render(user));
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadImage() {
        String username = session().get("username");
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart picture = body.getFile("picture");
        if (picture != null) {
            String fileName = picture.getFilename();
            String contentType = picture.getContentType();
            File file = picture.getFile();
            try {
                FileInputStream fis = new FileInputStream(file);
                File savedFile = new File(IMAGE_FILE_LOCATION + file.separator + username + ".jpg");
                FileOutputStream fos = new FileOutputStream(savedFile);
                FileUtils.copy(fis, fos);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }

            return ok("File uploaded");
        }
        else {
            flash("error", "Missing file");
            return redirect(routes.Application.userMain());
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result image(String name) {
        File image = new File(IMAGE_FILE_LOCATION, name + ".jpg");
        return ok(image);
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadJarPage() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        return ok(views.html.uploadJar.render(user));
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadJar() {
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart jar = body.getFile("jar");
        if (jar != null) {
            String fileName = jar.getFilename();
            String contentType = jar.getContentType();
            File file = jar.getFile();
            return ok("File uploaded");
        }
        else {
            flash("error", "Missing file");
            return redirect(routes.Application.userMain());
        }
    }

    /*@Security.Authenticated(Secured.class)
    public static Result userImage() {
        InputStream pictureStream;
        try {
            pictureStream = new FileInputStream(new File("storage/images/cowardly-lion.jpg"));
        }
        catch (Exception e)
        {
            return TODO;
        }

    }
    */

    public static Result viewGame(Long id) {
        return TODO;
    }

    public static Result deleteUser(Long id) {
        User.delete(id);
        return redirect(routes.Application.users());
    }

    public static class Login {
        @Required
        public String username;

        @Required
        public String password;

        public void setUsername(String username)
        {
            this.username = username;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String validate() {
            if (!User.authenticate(username, password)) {
                return "Invalid user or password";
            }
            return null;
        }
    }

    public static class AdminLogin {
        @Required
        public String username;

        @Required
        public String password;

        public void setUsername(String username)
        {
            this.username = username;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String validate() {
            if (!username.equals("walter"))
            {
                return "Invalid user or password";
            }
            if (!password.equals("corerul3z"))
            {
                return "Invalid user or password";
            }
            return null;
        }
    }
  
}