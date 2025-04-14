package at.primetshofer.services;

import at.primetshofer.logic.provider.file.UnicodeFilenameFileProvider;
import at.primetshofer.model.Controller;
import at.primetshofer.model.dto.DTOConverter;
import at.primetshofer.model.dto.JsonSerializer;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

public class NetworkLearningService extends Service<Void> {

    private static final Logger logger = Logger.getLogger(NetworkLearningService.class);

    private static final int UDP_BROADCAST_PORT = 9875;
    private static final int UDP_PORT = 9876;
    private static final int TCP_PORT = 9877;
    private final BooleanProperty deviceConnected;
    private DatagramSocket datagramSocket;
    private ServerSocket serverSocket;
    private final Controller controller;
    private final EntityManager entityManager;
    private boolean udpActive;

    public NetworkLearningService(BooleanProperty deviceConnected) {
        this.deviceConnected = deviceConnected;
        this.controller = Controller.getInstance();
        this.entityManager = HibernateUtil.getEntityManager();
        this.udpActive = false;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                udpActive = true;

                createUDPThread();

                startTCPServer();
                return null;
            }
        };
    }

    @Override
    public boolean cancel() {
        udpActive = false;
        datagramSocket.close();

        try {
            serverSocket.close();
        } catch (IOException ex) {
            logger.error("Failed to close server socket", ex);
            ViewUtils.showAlert(Alert.AlertType.ERROR, ex.getMessage(), LangController.getText("NetworkError"));
        }

        return super.cancel();
    }

    private void createUDPThread() {
        if (udpActive) {
            Thread udpThread = new Thread(() -> {
                startUDPListener();
            });
            udpThread.setDaemon(true);
            udpThread.start();
        }
    }

    public void fakeUDPBroadcast() {
        try {
            String replyMessage = "Server-Running";
            byte[] replyBuffer = replyMessage.getBytes();
            InetAddress senderAddress = InetAddress.getByName("10.0.1.2");

            DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length, senderAddress, UDP_PORT);

            datagramSocket.send(replyPacket);
        } catch (IOException ex) {
            logger.error("Failed to fake UDP broadcast", ex);
            throw new RuntimeException(ex);
        }
    }

    private void startUDPListener() {
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }

            datagramSocket = new DatagramSocket(UDP_BROADCAST_PORT);

            byte[] buffer = new byte[1024];

            while (udpActive) {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(receivePacket);

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress senderAddress = receivePacket.getAddress();

                if (receivedMessage.contains("JapaneseLearningApp-Server-Search")) {
                    String replyMessage = "Server-Running";
                    byte[] replyBuffer = replyMessage.getBytes();

                    DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length, senderAddress, UDP_PORT);

                    datagramSocket.send(replyPacket);
                }
            }
        } catch (Exception ex) {
            if (!ex.getMessage().equals("Socket closed")) {
                logger.error("Unexpected exception was thrown while starting UDP listener", ex);
                ViewUtils.showAlert(Alert.AlertType.ERROR, ex.getMessage(), LangController.getText("NetworkError"));
            }
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    private void startTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            this.serverSocket = serverSocket;
            logger.info("TCP Server started on port '" + TCP_PORT + "'. Waiting for connections...");

            boolean firstConnection = true;

            while (deviceConnected.get() || firstConnection) {
                try (Socket clientSocket = serverSocket.accept();
                     ObjectInputStream objectInput = new ObjectInputStream(clientSocket.getInputStream());
                     ObjectOutputStream objectOutput = new ObjectOutputStream(clientSocket.getOutputStream())) {

                    String command = (String) objectInput.readObject();
                    String[] commandParts = command.split(":");
                    command = commandParts[0];
                    String value = "";
                    if (commandParts.length > 1) {
                        for (int i = 1; i < commandParts.length; i++) {
                            value += commandParts[i];
                            if (i < commandParts.length - 1) {
                                value += ":";
                            }
                        }
                    }

                    logger.debug("Received command: " + command);

                    if (firstConnection) {
                        if (command.equals("CONNECT")) {
                            firstConnection = false;
                        } else {
                            continue;
                        }
                    }

                    switch (command) {
                        case "CONNECT":
                            deviceConnected.set(true);
                            objectOutput.writeObject("CONNECTED");
                            break;

                        case "GET_NEXT_KANJI":
                            Kanji kanji = controller.getNextLearningKanji();
                            objectOutput.writeObject(JsonSerializer.toJson(DTOConverter.kanjiToDTO(kanji)));

                            UnicodeFilenameFileProvider fileProvider = new UnicodeFilenameFileProvider(
                                    "kanjivg-20240807-all",
                                    '0',
                                    5,

                                    ".svg"
                            );
                            fileProvider.setCharForFilename(kanji.getSymbol().charAt(0));

                            String svgContent = new String(Files.readAllBytes(fileProvider.provideFile().getAbsoluteFile().toPath()));

                            objectOutput.writeObject(svgContent);
                            break;

                        case "GET_RANDOM_WORDS":
                            List<Word> wordList = controller.getRandomWordsFromKanjiTrainer(Integer.parseInt(value));

                            objectOutput.writeObject(JsonSerializer.toJson(DTOConverter.wordsToDTO(wordList, true)));

                            break;

                        case "GET_RANDOM_KANJIS":
                            List<Kanji> kanjiList = controller.getRandomKanji(Integer.parseInt(value));

                            objectOutput.writeObject(JsonSerializer.toJson(DTOConverter.kanjisToDTO(kanjiList)));

                            break;

                        case "GET_WORD_KANJIS":
                            Word word = entityManager.find(Word.class, Integer.parseInt(value));

                            if (word != null) {
                                objectOutput.writeObject(JsonSerializer.toJson(DTOConverter.kanjisToDTO(word.getKanjis())));
                            } else {
                                logger.error("Word not found");
                            }

                            break;

                        case "UPDATE_PROGRESS":
                            String[] arguments = value.split(";");
                            Kanji kanji1 = entityManager.find(Kanji.class, Integer.parseInt(arguments[0]));

                            controller.addKanjiProgress(kanji1, Integer.parseInt(arguments[1]));

                            break;

                        case "GET_AUDIO":
                            File audio = new File(value);

                            objectOutput.write(Files.readAllBytes(audio.getAbsoluteFile().toPath()));

                            break;

                        case "PLAY_AUDIO":
                            controller.playAudio(value);

                            break;

                        case "GET_DAILY_KANJI_INFO":
                            String response = controller.getDueKanjiCount() + ";" + controller.getDueTotalKanjiCount() + ";" + controller.getKanjiProgress();

                            objectOutput.writeObject(response);

                            break;

                        case "ADD_KANJI_TO_DUE":
                            controller.increaseDueKanjiTmp(Integer.parseInt(value));
                            controller.updateKanjiList();

                            objectOutput.writeObject("SUCCESS");

                            break;

                        case "ADD_TIME_STATS":
                            controller.addDurationToStats(Duration.ofMillis(Long.parseLong(value)));

                            break;

                        case "EXIT":
                            deviceConnected.set(false);
                            firstConnection = true;
                            logger.info("Client requested disconnection.");
                            break;

                        default:
                            objectOutput.writeObject("Unknown command: " + command);
                    }

                    if (!command.equals("CONNECT") && !command.equals("EXIT")) {
                        objectOutput.writeObject("CLOSE");
                    }

                } catch (Exception ex) {
                    if (!ex.getMessage().equals("Socket closed") && !ex.getMessage().equals("Socket is closed")) {
                        logger.error("Unexpected exception was thrown during communication", ex);
                        ViewUtils.showAlert(Alert.AlertType.ERROR, ex.getMessage(), LangController.getText("NetworkError"));
                    }
                    deviceConnected.set(false);
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to start TCP server", ex);
            ViewUtils.showAlert(Alert.AlertType.ERROR, ex.getMessage(), LangController.getText("NetworkError"));
            deviceConnected.set(false);
        }
    }
}
