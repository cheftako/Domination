package business;

import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Universe;
import com.linkedin.domination.server.Game;
import com.linkedin.domination.server.JsonWatcher;
import com.linkedin.domination.server.UniverseGenerator;
import com.linkedin.domination.server.Watcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/12/13
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameController
{
    public static void RunGame(Player playerOne, Player playerTwo, Player playerThree, File jsonFile)
    {
        UniverseGenerator generator = new UniverseGenerator(60, 1000, 800, 40);
        Universe universe = generator.createUniverse();

        int numberTurns = 1000;

        Map<Integer, Player> playerMap = new HashMap<Integer, Player>();
        playerMap.put(1, playerOne);
        playerMap.put(2, playerTwo);
        playerMap.put(3, playerThree);
        List<Watcher> watchers = new ArrayList<Watcher>();
        GameServerWatcher resultWatcher = new GameServerWatcher(jsonFile);
        watchers.add(resultWatcher);
        Game game = new Game(universe, playerMap, numberTurns, watchers);
        universe = game.start();
    }

    public static Class getPlayerClass(File jarFile) throws IOException
    {
        URL url = jarFile.toURL();
        //File gameApiJar = new File("lib/game-api.jar");
        URL[] urls = new URL[]{url /*, gameApiJar.toURL() */};
        ClassLoader playerLoader = new URLClassLoader(urls, GameController.class.getClassLoader());

        Class<?> playerClass = null;
        JarFile playerJar = new JarFile(jarFile);
        Enumeration<JarEntry> entries = playerJar.entries();

        while (entries.hasMoreElements())
        {
            JarEntry entry = entries.nextElement();

            String candidateClassName = entry.getName().replace('/', '.');
            if (!candidateClassName.endsWith("class"))
            {
                continue;
            }

            candidateClassName = candidateClassName.substring(0, candidateClassName.length() - 6);

            try
            {
                Class<?> candidate = playerLoader.loadClass(candidateClassName);

                if (com.linkedin.domination.api.Player.class.isAssignableFrom(candidate))
                {
                    if (playerClass != null)
                    {
                        System.out.println("Found multiple player classes " + playerClass.getCanonicalName() + " and " + candidate.getCanonicalName());
                        throw new IllegalStateException("Multiple org.linkedin.contest.game.player.Player classes found!");
                    }
                    playerClass = candidate;
                }
                else
                {
                    System.out.println(candidate.getName() + " is not an implementation of Player.");
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                // Ignore
                cnfe.printStackTrace();
                cnfe = null;
            }
        }

        return playerClass;
    }

    public static boolean isValidJar(File jarFile) throws IOException
    {
        return getPlayerClass(jarFile) != null;
    }
}
