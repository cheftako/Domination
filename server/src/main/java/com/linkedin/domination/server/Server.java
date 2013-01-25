package com.linkedin.domination.server;

import com.linkedin.domination.api.Universe;
import com.linkedin.domination.api.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: cmiller
 * Date: 1/12/13
 * Time: 5:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class Server
{
    private Universe _universe;

    private static boolean checkUsage(String[] args)
    {
        boolean havePlayer1 = false;
        boolean havePlayer2 = false;
        boolean havePlayer3 = false;
        for (int counter = 0; counter < args.length; counter++)
        {
            if (args[counter].equals("-H"))
            {
                return false;
            }
            else if (args[counter].equals("-p1"))
            {
                havePlayer1 = true;
                if (++counter >= args.length)
                {
                    return false;
                }
            }
            else if (args[counter].equals("-p2"))
            {
                havePlayer2 = true;
                if (++counter >= args.length)
                {
                    return false;
                }
            }
            else if (args[counter].equals("-p3"))
            {
                havePlayer3 = true;
                if (++counter >= args.length)
                {
                    return false;
                }
            }
            else if (args[counter].equals("-r"))
            {
                if (++counter >= args.length)
                {
                    return false;
                }
            }
            else if (args[counter].equals("-n"))
            {
                if (++counter >= args.length)
                {
                    return false;
                }
            }
            else if (args[counter].equals("-X"))
            {
            }
            else if (args[counter] != null)
            {
                System.out.println("Unrecognized option " + args[counter]);
                return false;
            }
        }
        return havePlayer1 && havePlayer2 && havePlayer3;
    }

    private static void printUsage()
    {
        System.out.println("Server [-HX] [-n number] -p1 player1 -p2 player2 -p3 player3");
        System.out.println("\t-H\tPrints this help message.");
        System.out.println("\t-X\tIgnore validity check on newly played words.");
        System.out.println("\t-p1 player1\tSpecify the full class for the first player");
        System.out.println("\t-p2 player2\tSpecify the full class for the second player");
        System.out.println("\t-p3 player3\tSpecify the full class for the third player");
        System.out.println("\t-n number\tNumber of turns in a game");
    }

    private static boolean checkValidity(String[] args)
    {
        for (int counter = 0; counter < args.length; counter++)
        {
            if (args[counter].equals("-X"))
            {
                return false;
            }
        }
        return true;
    }

    private static Player getPlayer(String[] args, String flag)
    {
        String className = null;
        for (int counter = 0; counter < args.length; counter++)
        {
            if (args[counter].equals(flag))
            {
                className = args[counter + 1];
                break;
            }
        }
        if (className == null || className.isEmpty())
        {
            throw new RuntimeException("Missing player class name");
        }
        try
        {
            Class<?> playerClass = Class.forName(className);
            Object result = playerClass.newInstance();
            if (!(result instanceof Player))
            {
                throw new RuntimeException("Instantiated class not of type player");
            }
            return (Player)result;
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Problem creating player", e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException("Problem creating player", e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Problem creating player", e);
        }
    }

    private static String getInitArg(String[] args, String key)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(key))
            {
                return args[i+1];
            }
        }
        return null;
    }

    private static Universe playersUniverse(Integer player, Universe universe)
    {
        return universe;
    }

    private static Player getPlayerOne(String[] args)
    {
        Player one = getPlayer(args, "-p1");
        one.initialize(1);
        return one;
    }

    private static Player getPlayerTwo(String[] args)
    {
        Player two = getPlayer(args, "-p2");
        two.initialize(2);
        return two;
    }

    private static Player getPlayerThree(String[] args)
    {
        Player three = getPlayer(args, "-p3");
        three.initialize(3);
        return three;
    }

    private static Integer getNumberTurns(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-n"))
            {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return new Integer(1000);
    }

    public static void main(String[] args)
    {
        //System.out.println("Starting the game server");
        if (!checkUsage(args))
        {
            printUsage();
            return;
        }
        boolean checkValidity = checkValidity(args);

        UniverseGenerator generator = new UniverseGenerator(60, 1000, 800, 40);
        Universe universe = generator.createUniverse();
        //System.out.println(universe.toString());

        Player playerOne = getPlayerOne(args);
        Player playerTwo = getPlayerTwo(args);
        Player playerThree = getPlayerThree(args);

        //System.out.println(playerOne.toString());
        //System.out.println(playerTwo.toString());
        //System.out.println(playerThree.toString());

        int numberTurns = getNumberTurns(args);

        Map<Integer, Player> playerMap = new HashMap<Integer, Player>();
        playerMap.put(1, playerOne);
        playerMap.put(2, playerTwo);
        playerMap.put(3, playerThree);
        Game game = new Game(universe, playerMap, numberTurns);
        universe = game.start();

        //System.out.println(universe);
    }

}