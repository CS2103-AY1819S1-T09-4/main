package seedu.address.ui;

import static seedu.address.logic.parser.CliSyntax.PREFIX_CAR_TYPE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NIGHT_PARKING;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PARKING_TIME;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.ui.NewResultAvailableEvent;
import seedu.address.logic.ListElementPointer;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.SelectCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    private static final String FXML = "CommandBox.fxml";
    private static final int INDEX_OF_FILTER_FIRST_ARG = 10;
    private static final int END_OF_FILTER_FIRST_ARG = 13;

    private final Logger logger = LogsCenter.getLogger(CommandBox.class);
    private final Logic logic;
    private ListElementPointer historySnapshot;

    //autocomplete variables
    private Set<String> autoCompleteCommands = new HashSet<>();
    private int anchorPosition;
    private int caretPosition;
    private String selectedText = "";

    @FXML
    private TextField commandTextField;

    public CommandBox(Logic logic) {
        super(FXML);
        this.logic = logic;
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        historySnapshot = logic.getHistorySnapshot();

        autoCompleteCommands.addAll(
            Arrays.asList(FindCommand.COMMAND_WORD, FindCommand.COMMAND_ALIAS));
        autoCompleteCommands.addAll(
            Arrays.asList(SelectCommand.COMMAND_WORD, SelectCommand.COMMAND_ALIAS));
        autoCompleteCommands.addAll(
            Arrays.asList(FilterCommand.COMMAND_WORD, FilterCommand.COMMAND_ALIAS));
    }

    /**
     * Handles the key press event, {@code keyEvent}.
     */
    @FXML
    private void handleKeyPress(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
        case UP:
            // As up and down buttons will alter the position of the caret,
            // consuming it causes the caret's position to remain unchanged
            keyEvent.consume();

            navigateToPreviousInput();
            break;
        case DOWN:
            keyEvent.consume();
            navigateToNextInput();
            break;
        case TAB:
            if (keyEvent.isShiftDown()) {
                //selectPreviousField();
            } else {
                autoComplete();
            }
            break;
        default:
            // let JavaFx handle the keypress
        }
    }

    /**
     * Handles autocomplete logic to either display full format, or select next
     * select next argument placeholder if full format is already displayed.
     */
    private void autoComplete() {
        String input = commandTextField.getText().trim().toLowerCase();
        if (autoCompleteCommands.contains(input)) { //auto-complete the formats
            displayFormat(input);

            // auto select first argument placeholder
            commandTextField.requestFocus();
            switch(input) {
            case FindCommand.COMMAND_WORD:
            case FindCommand.COMMAND_ALIAS:
            case SelectCommand.COMMAND_WORD:
            case SelectCommand.COMMAND_ALIAS:
                String text = commandTextField.getText();
                int indexOfFirstSpace = text.indexOf(" ");
                commandTextField.selectRange(
                    indexOfFirstSpace + 1, text.length());
                break;
            case FilterCommand.COMMAND_WORD:
            case FilterCommand.COMMAND_ALIAS:
                commandTextField.selectRange(
                    INDEX_OF_FILTER_FIRST_ARG, END_OF_FILTER_FIRST_ARG);
                caretPosition = commandTextField.getCaretPosition();
                break;
            default:
                break;
            }
        } else if (isFilterCommandFormat(input)) {
            int positionOfWeekDay = input.indexOf(PREFIX_PARKING_TIME.toString()) + 3;
            int positionOfStartTime = input.indexOf(PREFIX_PARKING_TIME.toString()) + 7;
            int positionOfEndTime = input.indexOf(PREFIX_PARKING_TIME.toString()) + 18;
            int positionOfNightParking = input.indexOf(PREFIX_NIGHT_PARKING.toString());
            int positionOfCarParkType = input.indexOf(PREFIX_CAR_TYPE.toString()) + 4;
            int[] argumentsArray = {positionOfWeekDay, positionOfStartTime,
                positionOfEndTime, positionOfNightParking, positionOfCarParkType};

            selectNextField(argumentsArray);
        }
    }

    /**
     * Displays the entire command format in command box
     * @param command input by the user
     */
    private void displayFormat(String command) {
        if (command.equals(FindCommand.COMMAND_WORD)
            || command.equals(FindCommand.COMMAND_ALIAS)) {
            replaceText(FindCommand.FORMAT);
        } else if (command.equals(SelectCommand.COMMAND_WORD)
            || command.equals(SelectCommand.COMMAND_ALIAS)) {
            replaceText(SelectCommand.FORMAT);
        } else if (command.equals(FilterCommand.COMMAND_WORD)
            || command.equals(FilterCommand.COMMAND_ALIAS)) {
            replaceText(FilterCommand.FORMAT);
        }
    }

    /**
     * autocomplete helper function to check if the text input is already in
     * filter command format.
     * @param input input by the user
     */
    private boolean isFilterCommandFormat(String input) {
        return input.startsWith("filter") && (input.contains("f/")
            || input.contains("n/") || input.contains("ct/"));
    }

    /**
     * Checks the current position is in between which two fields
     * And navigates to the next field
     * @param argumentsArray array of field positions in the order of left to right
     *                            last element is the end position of text input
     */
    private void selectNextField(int[] argumentsArray) {
        boolean updatedSelection = false;
        for (int i = 0; i < argumentsArray.length - 1; i++) {
            //check if the current position is in between ard[i] and ard[i + 1], if so, change selection
            //to the placeholder of ard[i + 1]
            System.out.println("caret at: " + caretPosition + " current at: " + argumentsArray[i]);
            if (caretPosition > argumentsArray[i] && caretPosition < argumentsArray[i + 1]) {
                commandTextField.positionCaret(argumentsArray[i + 1]);
                changeSelectionToNextField();
                updatedSelection = true;
            }
        }
        if (!updatedSelection) {
            //if caret position is not changed in the above for loop, it means
            //the caret is currently at the last field, then change selection to
            //the first arg so that continuously pressing tab will go in a cycle
            commandTextField.positionCaret(argumentsArray[0]);
            changeSelectionToNextField();
        }
        commandTextField.requestFocus();
        commandTextField.selectRange(anchorPosition, anchorPosition + selectedText.length());
        selectedText = "";
        caretPosition = commandTextField.getCaretPosition();
    }

    /**
     * Selects the word following the current caret position
     */
    private void changeSelectionToNextField() {
        commandTextField.selectNextWord();
        anchorPosition = commandTextField.getAnchor();
        selectedText = commandTextField.getSelectedText().trim();
    }

    /**
     * Updates the text field with the previous input in {@code historySnapshot},
     * if there exists a previous input in {@code historySnapshot}
     */
    private void navigateToPreviousInput() {
        assert historySnapshot != null;
        if (!historySnapshot.hasPrevious()) {
            return;
        }

        replaceText(historySnapshot.previous());
    }

    /**
     * Updates the text field with the next input in {@code historySnapshot},
     * if there exists a next input in {@code historySnapshot}
     */
    private void navigateToNextInput() {
        assert historySnapshot != null;
        if (!historySnapshot.hasNext()) {
            return;
        }

        replaceText(historySnapshot.next());
    }

    /**
     * Sets {@code CommandBox}'s text field with {@code text} and
     * positions the caret to the end of the {@code text}.
     */
    private void replaceText(String text) {
        commandTextField.setText(text);
        commandTextField.positionCaret(commandTextField.getText().length());
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        try {
            CommandResult commandResult = logic.execute(commandTextField.getText());
            initHistory();
            historySnapshot.next();
            // process result of the command
            commandTextField.setText("");
            logger.info("Result: " + commandResult.feedbackToUser);
            raise(new NewResultAvailableEvent(commandResult.feedbackToUser));

        } catch (CommandException | ParseException e) {
            initHistory();
            // handle command failure
            setStyleToIndicateCommandFailure();
            logger.info("Invalid command: " + commandTextField.getText());
            raise(new NewResultAvailableEvent(e.getMessage()));
        }
    }

    /**
     * Initializes the history snapshot.
     */
    private void initHistory() {
        historySnapshot = logic.getHistorySnapshot();
        // add an empty string to represent the most-recent end of historySnapshot, to be shown to
        // the user if she tries to navigate past the most-recent end of the historySnapshot.
        historySnapshot.add("");
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

}