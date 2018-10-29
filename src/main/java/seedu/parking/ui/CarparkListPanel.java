package seedu.parking.ui;

import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.parking.commons.core.LogsCenter;
import seedu.parking.commons.events.ui.CarparkPanelSelectionChangedEvent;
import seedu.parking.commons.events.ui.JumpToListRequestEvent;
import seedu.parking.commons.events.ui.NotifyCarparkRequestEvent;
import seedu.parking.model.carpark.Carpark;

/**
 * Panel containing the list of car parks.
 */
public class CarparkListPanel extends UiPart<Region> {
    private static final String FXML = "CarparkListPanel.fxml";
    private static int selectIndex = -1;
    private final Logger logger = LogsCenter.getLogger(CarparkListPanel.class);

    @FXML
    private ListView<Carpark> carparkListView;

    public CarparkListPanel(ObservableList<Carpark> carparkList) {
        super(FXML);
        setConnections(carparkList);
        registerAsAnEventHandler(this);
    }

    private void setConnections(ObservableList<Carpark> carparkList) {
        carparkListView.setItems(carparkList);
        carparkListView.setCellFactory(listView -> new CarparkListViewCell());
        setEventHandlerForSelectionChangeEvent();
    }

    private void setEventHandlerForSelectionChangeEvent() {
        carparkListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        logger.fine("Selection in car park list panel changed to : '" + newValue + "'");
                        selectIndex = carparkListView.getSelectionModel().getSelectedIndex();
                        raise(new CarparkPanelSelectionChangedEvent(newValue));
                    }
                });
    }

    /**
     * Scrolls to the {@code CarparkCard} at the {@code index} and selects it.
     */
    private void scrollTo(int index) {
        Platform.runLater(() -> {
            carparkListView.scrollTo(index);
            carparkListView.getFocusModel().focus(index);
            carparkListView.getSelectionModel().clearAndSelect(index);
        });
    }

    @Subscribe
    private void handleJumpToListRequestEvent(JumpToListRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        scrollTo(event.targetIndex);
    }

    @Subscribe
    private void handleNotifyCarparkRequestEvent(NotifyCarparkRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));


    }

    /**
     * Get the selected car park from the list view.
     * @return the index of the selected car park.
     */
    public static int getSelectedIndex() {
        return selectIndex;
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Carpark} using a {@code CarparkCard}.
     */
    class CarparkListViewCell extends ListCell<Carpark> {
        @Override
        protected void updateItem(Carpark carpark, boolean empty) {
            super.updateItem(carpark, empty);

            if (empty || carpark == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new CarparkCard(carpark, getIndex() + 1).getRoot());
            }
        }
    }

}