/* SPDX-License-Identifier: MIT */
package atlantafx.sampler.page.general;

import atlantafx.base.theme.Styles;
import atlantafx.sampler.event.DefaultEventBus;
import atlantafx.sampler.event.ThemeEvent;
import atlantafx.sampler.page.AbstractPage;
import atlantafx.sampler.theme.SamplerTheme;
import atlantafx.sampler.theme.ThemeManager;
import atlantafx.sampler.util.NodeUtils;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import static atlantafx.sampler.event.ThemeEvent.EventType.*;
import static atlantafx.sampler.util.Controls.hyperlink;

public class ThemePage extends AbstractPage {

    public static final String NAME = "Theme";
    private static final ThemeManager TM = ThemeManager.getInstance();

    private final Consumer<ColorPaletteBlock> colorBlockActionHandler = colorBlock -> {
        ContrastCheckerDialog dialog = getOrCreateContrastCheckerDialog();
        dialog.getContent().setValues(
                colorBlock.getFgColorName(),
                colorBlock.getFgColor(),
                colorBlock.getBgColorName(),
                colorBlock.getBgColor()
        );
        overlay.setContent(dialog, HPos.CENTER);
        overlay.toFront();
    };

    private final ColorPalette colorPalette = new ColorPalette(colorBlockActionHandler);
    private final ColorScale colorScale = new ColorScale();
    private final ChoiceBox<SamplerTheme> themeSelector = themeSelector();

    private ThemeRepoManagerDialog themeRepoManagerDialog;
    private ContrastCheckerDialog contrastCheckerDialog;

    @Override
    public String getName() { return NAME; }

    public ThemePage() {
        super();
        createView();
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, e -> {
            if (e.getEventType() == THEME_ADD || e.getEventType() == THEME_REMOVE) {
                themeSelector.getItems().setAll(TM.getRepository().getAll());
                selectCurrentTheme();
            }
            if (e.getEventType() == THEME_CHANGE || e.getEventType() == COLOR_CHANGE) {
                colorPalette.updateColorInfo(Duration.seconds(1));
                colorScale.updateColorInfo(Duration.seconds(1));
            }
        });
    }

    @Override
    protected void onRendered() {
        super.onRendered();
        colorPalette.updateColorInfo(Duration.ZERO);
        colorScale.updateColorInfo(Duration.ZERO);
    }

    private void createView() {
        var noteText = new TextFlow(
                new Text("AtlantaFX follows "),
                hyperlink("Github Primer interface guidelines",
                          URI.create("https://primer.style/design/foundations/color")
                ),
                new Text(" and color system.")
        );

        userContent.getChildren().addAll(
                optionsGrid(),
                noteText,
                colorPalette,
                colorScale
        );

        selectCurrentTheme();

        // if you want to enable quick menu don't forget that
        // theme selection choice box have to be updated accordingly
        NodeUtils.toggleVisibility(quickConfigBtn, false);
        NodeUtils.toggleVisibility(sourceCodeToggleBtn, false);
    }

    private GridPane optionsGrid() {
        var themeRepoBtn = new Button("", new FontIcon(Material2OutlinedMZ.SETTINGS));
        themeRepoBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        themeRepoBtn.setTooltip(new Tooltip("Settings"));
        themeRepoBtn.setOnAction(e -> {
            ThemeRepoManagerDialog dialog = getOrCreateThemeRepoManagerDialog();

            overlay.setContent(dialog, HPos.CENTER);
            dialog.getContent().update();

            overlay.toFront();
        });

        var accentSelector = new AccentColorSelector();

        // ~

        var grid = new GridPane();
        grid.setVgap(20);
        grid.setHgap(20);

        grid.add(new Label("Color theme"), 0, 0);
        grid.add(themeSelector, 1, 0);
        grid.add(themeRepoBtn, 2, 0);
        grid.add(new Label("Accent color"), 0, 1);
        grid.add(accentSelector, 1, 1);

        return grid;
    }

    private ChoiceBox<SamplerTheme> themeSelector() {
        var selector = new ChoiceBox<SamplerTheme>();
        selector.getItems().setAll(TM.getRepository().getAll());
        selector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && getScene() != null) { TM.setTheme(val); }
        });
        selector.setPrefWidth(250);

        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(SamplerTheme theme) {
                return theme != null ? theme.getName() : "";
            }

            @Override
            public SamplerTheme fromString(String themeName) {
                return TM.getRepository().getAll().stream()
                        .filter(t -> Objects.equals(themeName, t.getName()))
                        .findFirst()
                        .orElse(null);
            }
        });

        return selector;
    }

    private void selectCurrentTheme() {
        if (TM.getTheme() == null) { return; }
        themeSelector.getItems().stream()
                .filter(t -> Objects.equals(TM.getTheme().getName(), t.getName()))
                .findFirst()
                .ifPresent(t -> themeSelector.getSelectionModel().select(t));
    }

    private ThemeRepoManagerDialog getOrCreateThemeRepoManagerDialog() {
        if (themeRepoManagerDialog == null) {
            themeRepoManagerDialog = new ThemeRepoManagerDialog();
        }

        themeRepoManagerDialog.setOnCloseRequest(() -> {
            overlay.removeContent();
            overlay.toBack();
        });

        return themeRepoManagerDialog;
    }

    private ContrastCheckerDialog getOrCreateContrastCheckerDialog() {
        if (contrastCheckerDialog == null) {
            contrastCheckerDialog = new ContrastCheckerDialog(colorPalette.bgBaseColorProperty());
        }

        contrastCheckerDialog.setOnCloseRequest(() -> {
            overlay.removeContent();
            overlay.toBack();
        });

        return contrastCheckerDialog;
    }
}
