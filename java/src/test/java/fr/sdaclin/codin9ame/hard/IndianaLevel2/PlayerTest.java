package fr.sdaclin.codin9ame.hard.IndianaLevel2;

import org.testng.annotations.Test;

public class PlayerTest {

    @Test
    public void testLvl1() {
        Player.Configuration configuration = Player.Configuration.forDump("5 3\n" +
                "0 0 -3 0 0\n" +
                "0 0 2 0 0\n" +
                "0 0 -3 0 0\n" +
                "2");
        Player.Solver solver = new Player.Solver(configuration);
        solver.setIndyState(new Player.Coordinate(2, 0), "TOP");
        while (solver.printNextCommand()) {
        }
        ;
    }

    @Test
    public void testLvl2() {
        Player.Configuration configuration = Player.Configuration.forDump("8 4\n" +
                "0 -3 0 0 0 0 0 0\n" +
                "0 12 3 3 2 3 12 0\n" +
                "0 0 0 0 0 0 2 0\n" +
                "0 -12 3 2 2 3 13 0\n" +
                "1");
        Player.Solver solver = new Player.Solver(configuration);
        solver.setIndyState(new Player.Coordinate(1, 0), "TOP");
        while (solver.printNextCommand()) {
        }
    }

    @Test
    public void testLvl3() {
        Player.Configuration configuration = Player.Configuration.forDump("6 9\n" +
                "0 0 0 0 0 -3\n" +
                "8 3 3 2 2 10\n" +
                "2 0 0 0 10 13\n" +
                "11 3 -2 3 1 13\n" +
                "-3 10 0 0 2 0\n" +
                "0 6 3 3 4 13\n" +
                "0 3 0 13 -4 10\n" +
                "0 13 2 4 10 0\n" +
                "0 0 0 -3 0 0\n" +
                "3");
        Player.Solver solver = new Player.Solver(configuration);
        solver.setIndyState(new Player.Coordinate(5, 0), "TOP");
        while (solver.printNextCommand()) {
        }
    }

    @Test
    public void testRotation() {
        for (Player.Solver.RoomType roomType : Player.Solver.RoomType.values()) {
            Player.Solver.RoomType intermediateState = roomType.applyRotation(Player.Solver.Command.Verb.LEFT);
            Player.Solver.RoomType finalState = intermediateState.applyRotation(Player.Solver.Command.Verb.RIGHT);
            assert roomType.equals(finalState);
        }
    }
}