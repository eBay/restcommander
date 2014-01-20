package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        Logger.debug("I am a debug message");
        Logger.info("I am an info message");
        render();
    }

}