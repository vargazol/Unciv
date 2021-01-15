package com.unciv.ui.saves

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Json
import com.unciv.UncivGame
import com.unciv.logic.GameSaver
import com.unciv.models.translations.tr
import com.unciv.ui.pickerscreens.PickerScreen
import com.unciv.ui.utils.*
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread
import com.unciv.ui.utils.AutoScrollPane as ScrollPane


class SaveGameScreen : PickerScreen() {
    val gameNameTextField = TextField("", skin)
    val currentSaves = Table()

    init {
        setDefaultCloseAction()

        gameNameTextField.textFieldFilter = TextField.TextFieldFilter { _, char -> char != '\\' && char != '/' }
        currentSaves.add("Current saves".toLabel()).row()
        updateShownSaves(false)
        topTable.add(ScrollPane(currentSaves)).height(stage.height * 2 / 3)

        val newSave = Table()
        val defaultSaveName = game.gameInfo.currentPlayer + " -  " + game.gameInfo.turns + " turns"
        gameNameTextField.text = defaultSaveName

        newSave.add("Saved game name".toLabel()).row()
        newSave.add(gameNameTextField).width(300f).pad(10f).row()

        val copyJsonButton = "Copy to clipboard".toTextButton()
        copyJsonButton.onClick {
            val json = Json().toJson(game.gameInfo)
            val base64Gzip = Gzip.zip(json)
            Gdx.app.clipboard.contents = base64Gzip
        }
        newSave.add(copyJsonButton).row()
        if (GameSaver.canLoadFromCustomSaveLocation()) {
            val saveToCustomLocation = "Save to custom location".toTextButton()
            val errorLabel = "".toLabel(Color.RED)
            saveToCustomLocation.enable()
            saveToCustomLocation.onClick {
                errorLabel.setText("")
                saveToCustomLocation.setText("Saving...".tr())
                saveToCustomLocation.disable()
                thread(name = "SaveGame") {
                    GameSaver.saveGameToCustomLocation(UncivGame.Current.gameInfo, gameNameTextField.text) { e ->
                        if (e == null) {
                            Gdx.app.postRunnable { UncivGame.Current.setWorldScreen() }
                        } else if (e !is CancellationException) {
                            errorLabel.setText("Could not save game to custom location".tr())
                            e.printStackTrace()
                        }
                        saveToCustomLocation.enable()
                    }
                }
            }
            newSave.add(saveToCustomLocation).pad(10f).row()
            newSave.add(errorLabel).pad(0f, 10f, 10f, 10f).row()
        }


        val showAutosavesCheckbox = CheckBox("Show autosaves".tr(), skin)
        showAutosavesCheckbox.isChecked = false
        showAutosavesCheckbox.onChange {
            updateShownSaves(showAutosavesCheckbox.isChecked)
        }
        newSave.add(showAutosavesCheckbox).row()

        topTable.add(newSave)
        topTable.pack()

        rightSideButton.setText("Save game".tr())
        rightSideButton.onClick {
            if (GameSaver.getSave(gameNameTextField.text).exists())
                YesNoPopup("Overwrite existing file?", { saveGame() }, this).open()
            else saveGame()
        }
        rightSideButton.enable()
    }

    private fun saveGame() {
        rightSideButton.setText("Saving...".tr())
        thread(name = "SaveGame") {
            GameSaver.saveGame(UncivGame.Current.gameInfo, gameNameTextField.text) {
                Gdx.app.postRunnable { UncivGame.Current.setWorldScreen() }
            }
        }
    }

    fun updateShownSaves(showAutosaves: Boolean) {
        currentSaves.clear()
        val saves = GameSaver.getSaves()
                .sortedByDescending { it.lastModified() }
        for (saveGameFile in saves) {
            if (saveGameFile.name().startsWith("Autosave") && !showAutosaves) continue
            val textButton = saveGameFile.name().toTextButton()
            textButton.onClick {
                gameNameTextField.text = saveGameFile.name()
            }
            currentSaves.add(textButton).pad(5f).row()
        }
    }

}