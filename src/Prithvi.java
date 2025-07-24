/**
 * Prithvi – An in-memory key-value database
 * with TTL, LRU eviction, disk persistence, lists, sets,
 * and multithreaded client support.
 *
 * © 2025 PHILKHANA SIDHARTH
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy at:
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * You must retain this header in any redistribution or modification.
 */

package src;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import src.commands.Command;
import src.commands.CommandExecutor;
import src.commands.CommandMap;
import src.commands.utils.SaveCommand;
import src.db.Store;
import src.db.persistence.AutoLoad;
import src.db.persistence.AutoSaveTask;
import src.db.persistence.ExpiredKeyRemover;
import src.db.persistence.WALReplayer;
import src.metrics.MetricsServer;

public class Prithvi {
    public static final long START_TIME = System.currentTimeMillis();
    private static final int PORT = 1902;
    private static final Map<Command.Type, CommandExecutor> commandMap = CommandMap.getCommandMap();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("""


                     ██████╗ ██████╗ ██╗████████╗██╗  ██╗██╗   ██╗██╗
                     ██╔══██╗██╔══██╗██║╚══██╔══╝██║  ██║██║   ██║██║
                     ██████╔╝██████╔╝██║   ██║   ███████║██║   ██║██║
                     ██╔═══╝ ██╔══██╗██║   ██║   ██╔══██║╚██╗ ██╔╝██║
                     ██║     ██║  ██║██║   ██║   ██║  ██║ ╚████╔╝ ██║
                     ╚═╝     ╚═╝  ╚═╝╚═╝   ╚═╝   ╚═╝  ╚═╝  ╚═══╝  ╚═╝

                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    Prithvi     : Lightweight Key-Value Database (Alpha)
                    Author      : Philkhana Sidharth
                    Language    : Java (no frameworks)
                    Port        : 1902
                    Launched    : 14 Jul 2025
                    Persistence : True
                    Security    : JWT Token SHA-256 (BASIC)

                      Warning: This is an experimental build.
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    """);

            System.out.println("🚀 PrithviServer listening on port " + PORT);

            WALReplayer replayer = new WALReplayer(commandMap);

            replayer.replay();
            MetricsServer.start(9100);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n--Caught SIGINT (Ctrl+C). Saving store to disk...");
                try {
                    new SaveCommand().execute(
                            new Command(Command.Type.SAVE, null, null),
                            new PrintWriter(System.out, true),
                            null,
                            Store.get());

                    System.out.println("Store saved successfully.");

                } catch (Exception e) {
                    System.err.println("Failed to save on shutdown: " + e.getMessage());
                }
                System.out.println("👋 Shutdown complete. Exiting...");
            }));
            new Thread(new AutoLoad()).start();
            new Thread(new AutoSaveTask(300)).start();
            new Thread(new ExpiredKeyRemover(5, Store.get())).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("⚡ New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}