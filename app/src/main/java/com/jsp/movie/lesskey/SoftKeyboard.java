/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Some Hangul InputMethod Code added by www.kandroid.org
 * 
 */

package com.jsp.movie.lesskey;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;

    private Keyboard mHangulKeyboard; // Hangul Code
    private Keyboard mHangulShiftedKeyboard; // Hangul Code

    private Keyboard mHangulQwertyKeyboard; // Hangul Code
    private Keyboard mHangulQwertyShiftedKeyboard; // Hangul Code

    private Keyboard mCurKeyboard;

    private String mWordSeparators;

    public DatabaseHelper dbHelper;
    public SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    public String previous = null;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mWordSeparators = getResources().getString(R.string.word_separators);
        dbHelper = new DatabaseHelper(this);
        try {
            dbHelper.createDataBase();
        } catch (Exception e) {

            e.printStackTrace();
        }
        db = dbHelper.openDataBase();
        dbHandler = new DatabaseHandler(db);
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        Log.d("init", "aaaaaaaaaaaaa1");
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        mHangulKeyboard = new HangulKeyboard(this, R.xml.hangul);
        mHangulShiftedKeyboard = new HangulKeyboard(this, R.xml.hangul_shift);

        mHangulQwertyKeyboard = new HangulKeyboard(this, R.xml.hangul_qwerty);
        mHangulQwertyShiftedKeyboard = new HangulKeyboard(this, R.xml.hangul_qwerty_shift);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        Log.d("init", "aaaaaaaaaaaaa2");
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mHangulKeyboard);
        mInputView.setPreviewEnabled(false);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */

    @Override public View onCreateCandidatesView() {
        Log.d("init", "aaaaaaaaaaaaa3");
        mCandidateView = new CandidateView(this, dbHandler, previous);
        mCandidateView.setService(this);
        return mCandidateView;
    }
    @Override public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
/*        
        Log.i("Hangul", "onStartInput");
*/
        clearHangul();
        previousCurPos = -1;

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        LatinKeyboard curKeyboard = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                curKeyboard = mSymbolsKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                curKeyboard = mSymbolsKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mHangulKeyboard;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mCurKeyboard = mQwertyKeyboard;
                    mPredictionOn = false;
                }

                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
//                  mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mHangulKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        if (curKeyboard != null) {

            curKeyboard.setImeOptions(getResources(), attribute.imeOptions);
            mCurKeyboard = curKeyboard;
        }
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();

//        Log.d ("finish", getCurrentInputConnection().toString());

        if (prev.length() > 0)
            addNewText(prev);

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mHangulKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }


    /**
     * Deal with the editor reporting movement of its cursor.
     */

    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                            int newSelStart, int newSelEnd,
                                            int candidatesStart, int candidatesEnd) {

        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

/*        Log.i("Hangul", "onUpdateSelection :"
                        + Integer.toString(oldSelStart) + ":"
                        + Integer.toString(oldSelEnd) + ":"
                        + Integer.toString(newSelStart) + ":"
                        + Integer.toString(newSelEnd) + ":"
                        + Integer.toString(candidatesStart) + ":"
                        + Integer.toString(candidatesEnd)
        );*/

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        Keyboard current = mInputView.getKeyboard();
        if (current == mHangulKeyboard || current == mHangulShiftedKeyboard) {
            if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                    || newSelEnd != candidatesEnd)) {
                mComposing.setLength(0);
                updateCandidates();
                clearHangul();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.finishComposingText();
                }
            }
        }
        else if (current == mHangulQwertyKeyboard || current == mHangulQwertyShiftedKeyboard) {

        }
        else {
            if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                    || newSelEnd != candidatesEnd)) {
                mComposing.setLength(0);
                updateCandidates();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.finishComposingText();
                }
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {

        if (completions == null)
            return;

        Log.d("Movie", "Completions:" + completions.length);
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("Hangul", "onKeyDown :" + Integer.toString(keyCode));

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    void addNewText (String str) {

        Log.d ("AddNewText", str);
        new ChatParser(dbHandler).execute(str);
    }

    // Implementation of KeyboardViewListener

    String prev = "";

    public void onKey(int primaryCode, int[] keyCodes) {
//        Log.i("Hangul", "onKey PrimaryCode[" + Integer.toString(primaryCode)+"]");

        Keyboard current = mInputView.getKeyboard();

        if (isWordSeparator(primaryCode)) {
            // Handle separator

            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }

            if (current == mHangulKeyboard || current == mHangulShiftedKeyboard ) {
                clearHangul();
                sendKey(primaryCode);
            }
            else if (current == mHangulQwertyKeyboard || current == mHangulQwertyShiftedKeyboard) {
                clearHangul();
                sendKey(primaryCode);
            }
            else {
                sendKey(primaryCode);
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {

            if (current == mHangulKeyboard || current == mHangulShiftedKeyboard ) {
                hangulSendKey(-2,HCURSOR_NONE);
            }
            else if (current == mHangulQwertyKeyboard || current == mHangulQwertyShiftedKeyboard) {
                hangulQwertySendKey(-2,HCURSOR_NONE);
            }
            else {
                handleBackspace();
            }
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
            handleClose();
            return;
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {

            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                current = mHangulKeyboard;
            }
            // Hangul Start Code
            else if (current == mQwertyKeyboard) {
                if (mComposing.length() > 0) {
                    commitTyped(getCurrentInputConnection());
                }
                clearHangul();
                current = mHangulKeyboard;
            }
            // Hangul End Code
            else if (current == mHangulKeyboard || current == mHangulShiftedKeyboard ||
                    current == mHangulQwertyKeyboard || current == mHangulQwertyShiftedKeyboard) {
                mInputView.setShifted(false);
                if (mComposing.length() > 0) {
                    getCurrentInputConnection().commitText(mComposing, mComposing.length());
                    mComposing.setLength(0);
                }
                clearHangul();
                current = mSymbolsKeyboard;
            }
            mInputView.setKeyboard(current);
            if (current == mSymbolsKeyboard) {
                current.setShifted(false);
            }
        }
        else if (primaryCode == -10)
        {
            mInputView.setShifted(false);
            if (mComposing.length() > 0) {
                getCurrentInputConnection().commitText(mComposing, mComposing.length());
                mComposing.setLength(0);
            }
            clearHangul();
            mInputView.setKeyboard(mQwertyKeyboard);
        }
        else if (primaryCode == -11)
        {

            if (current == mHangulKeyboard)
                current = mHangulQwertyKeyboard;
            else if (current == mHangulShiftedKeyboard)
                current = mHangulQwertyShiftedKeyboard;
            else if (current == mHangulQwertyKeyboard)
                current = mHangulKeyboard;
            else if (current == mHangulQwertyShiftedKeyboard)
                current = mHangulShiftedKeyboard;
            clearHangul();
            mInputView.setKeyboard(current);
        }
        else {
            /* original code 
            handleCharacter(primaryCode, keyCodes);
            */

            // Hangul Start Code

            if (current == mHangulKeyboard || current == mHangulShiftedKeyboard) {
                handleHangul(primaryCode, keyCodes);
            }
            else if (current == mHangulQwertyKeyboard || current == mHangulQwertyShiftedKeyboard) {
                handleHangulQwerty(primaryCode, keyCodes);
            }
            else {
                handleCharacter(primaryCode, keyCodes);
            }
            // Hangul End Code
        }

        InputConnection ic = getCurrentInputConnection();
        String curr = "" + ic.getTextBeforeCursor(100, 0) + ic.getTextAfterCursor(100, 0);
        Log.d ("db", "curr:" + curr);

        if (curr.length() <= 1 && prev.length() > 0)
            addNewText(prev);

        prev = curr;
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        Log.d ("Movie", "handleBackspace");
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        }
        // Hangul Code Start
        else if (currentKeyboard == mHangulKeyboard) {
            mHangulKeyboard.setShifted(true);
            mInputView.setKeyboard(mHangulShiftedKeyboard);
            mHangulShiftedKeyboard.setShifted(true);
            mHangulShiftState = 1;
        } else if (currentKeyboard == mHangulShiftedKeyboard) {
            mHangulShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mHangulKeyboard);
            mHangulKeyboard.setShifted(false);
            mHangulShiftState = 0;
        }
        else if (currentKeyboard == mHangulQwertyKeyboard) {
            mHangulQwertyKeyboard.setShifted(true);
            mInputView.setKeyboard(mHangulQwertyShiftedKeyboard);
            mHangulQwertyShiftedKeyboard.setShifted(true);
            mHangulShiftState = 1;
        } else if (currentKeyboard == mHangulQwertyShiftedKeyboard) {
            mHangulQwertyShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mHangulQwertyKeyboard);
            mHangulQwertyKeyboard.setShifted(false);
            mHangulShiftState = 0;
        }
        // Hangul Code End
        else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

// Hangul Code Start

    private int isHangulKey(int stack_pos, int new_key) {
        /*    
        MAP(0,20,1); // ��,�� 
        MAP(3,23,4); // ��,��
        MAP(3,29,5); // ��,��
        MAP(8,0,9); // ��,��
        MAP(8,16,10); // ��,��
        MAP(8,17,11); // ��,��
        MAP(8,20,12); // ��,��
        MAP(8,27,13); // ��,��
        MAP(8,28,14); // ��,��
        MAP(8,29,15); // ��,��
        MAP(17,20,19); // ��,��         
    */
        if (stack_pos != 2) {
            switch (mHangulKeyStack[stack_pos]) {
                case 0:
                    if (new_key == 20) return 2;
                    break;
                case 3:
                    if (new_key == 23) return 4;
                    else if(new_key == 29) return 5;
                    break;
                case 8:
                    if (new_key == 0)return 9;
                    else if (new_key == 16) return 10;
                    else if (new_key == 17) return 11;
                    else if (new_key == 20) return 12;
                    else if (new_key == 27) return 13;
                    else if (new_key == 28) return 14;
                    else if (new_key == 29) return 15;
                    break;
                case 17:
                    if (new_key == 20) return 19;
                    break;
            }
        }
        else {
            /*           
            38, 30, 39 // �� �� ��
            38, 31, 40 // �� �� ��
            38, 50, 41 //�� �� ��
            43, 34, 44 // �� �� ��
            43, 35, 45 // �� �� ��
            43, 50, 46 // �� �� ��
            48, 50, 49 // �� �� ��
*/
            switch (mHangulKeyStack[stack_pos]) {
                case 38:
                    if (new_key == 30) return 39;
                    else if (new_key == 31) return 40;
                    else if (new_key == 50) return 41;
                    break;
                case 43:
                    if (new_key == 34) return 44;
                    else if (new_key == 35) return 45;
                    else if (new_key == 50) return 46;
                    break;
                case 48:
                    if (new_key == 50) return 49;
                    break;
            }
        }
        return 0;
    }

    private static char HCURSOR_NONE = 0;
    private static char HCURSOR_NEW = 1;
    private static char HCURSOR_ADD = 2;
    private static char HCURSOR_UPDATE = 3;
    private static char HCURSOR_APPEND = 4;
    private static char HCURSOR_UPDATE_LAST = 5;
    private static char HCURSOR_DELETE_LAST = 6;
    private static char HCURSOR_DELETE = 7;


    private static int mHCursorState = HCURSOR_NONE;
    private static char h_char[] = new char[1];
    private int previousCurPos = -2;
    private int previousHangulCurPos = -1;
    private int mHangulShiftState = 0;
    private int mHangulState = 0;
    private static int mHangulKeyStack[] = {0,0,0,0,0,0}; // ��,��,��,��,��,��
    private static int mHangulJamoStack[] = {0,0,0};
    final static int H_STATE_0 = 0;
    final static int H_STATE_1 = 1;
    final static int H_STATE_2 = 2;
    final static int H_STATE_3 = 3;
    final static int H_STATE_4 = 4;
    final static int H_STATE_5 = 5;
    final static int H_STATE_6 = 6;
    final static char[] h_chosung_idx =
            {0,1, 9,2,12,18,3, 4,5, 0, 6,7, 9,16,17,18,6, 7, 8, 9,9,10,11,12,13,14,15,16,17,18};
    /*
        {0, 1, 9, 2,12,18, 3,4, 5, 0, 6, 7, 9,16,17, 18,6, 7, 8, 9, 9,10,11, 12, 13,14,15,16,17,18};
    //   ��,��,��,��,��,��,��,��,��,��,��,��,��,��,��, ��,��,��,��,��,��,��,��, ��, ��,��,��, ��,��,��
    //   ��,��,   ��,      ��,��,��,                     ��,��,��,   ��,��,��, ��, ��,��,��, ��,��,��
    */
    final static char[] h_jongsung_idx =
            {0, 1, 2, 3,4,5, 6, 7, 0,8, 9,10,11,12,13,14,15,16,17,0 ,18,19,20,21,22,0 ,23,24,25,26,27};
/*
    {0, 1, 2, 3, 4, 5, 6, 7, 0,8, 9,10,11, 12,13, 14,15,16, 17,0,18, 19,20,21,22, 0 ,23,24,25,26,27};
//   x, ��,��,��,��,��,��,��,��,��,��,��,��, ��,��, ��,��,��, ��,��,��, ��,��, o,��, ��,��, ��,��,��,��,
    //  x  ��  ��  ��  ��  ��  ��  ��       ��  ��  ��  ��    ��  ��   ��   ��  ��    ��        ��   ��  ��   ��   ��         ��   ��   ��  ��  ��  

    //  ��,��,��,   ��,��,��,   ��,��,��,��,��,��,��,��,��,��,��,��,��,��,��
*/

    final static int[] e2h_map =
            {16,47,25,22,6, 8,29,38,32,34,30,50,48,43,31,35,17,0, 3,20,36,28,23,27,42,26,
                    16,47,25,22,7, 8,29,38,32,34,30,50,48,43,33,37,18,1, 3,21,36,28,24,27,42,26};
  /*
//   ��, ��,��, ��,��,��,��,��, ��,��, ��,��,��,��, ��,��, ��,��,��,��,��,��, ��,��,��, ��,
    {16,47,25, 22,6, 8,29,38, 32,34, 30,50,48,43,31,35, 17,0, 3, 20,36,28, 23,27,42,26,
//   ��, ��,��, ��,��,��,��,��, ��,��, ��,��,��,��, ��,��, ��,��,��,��,��,��, ��,��,��, ��
     16,47,25, 22,7, 8,29,38, 32,34, 30,50,48,43,33,37, 18,1, 3, 21,36,28, 24,27,42,26}; 
   */ 
/*
    final static char[] h_key_type =
//      0                            1                            2 
//      0, 1, 2, 3, 4, 5, 6, 7,8, 9, 0, 1, 2, 3, 4, 5, 6,7, 8, 9, 0, 1, 2, 3, 4,5, 6,7, 8, 9,
//      ��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��, o,��,��,��,��,��,��,��,
        { };
//      3                            4      
//      0, 1, 2, 3, 4, 5,6, 7, 8, 9, 0, 1, 2, 3,4, 5, 6, 7, 8, 9, 0     
//      ��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��,��

    0x3131,��
    0x314F,�� 
*/


    private void clearHangul() {
        mHCursorState = HCURSOR_NONE;
        mHangulState = 0;
        previousHangulCurPos = -1;
        mHangulKeyStack[0] = 0;
        mHangulKeyStack[1] = 0;
        mHangulKeyStack[2] = 0;
        mHangulKeyStack[3] = 0;
        mHangulKeyStack[4] = 0;
        mHangulKeyStack[5] = 0;
        mHangulJamoStack[0] = 0;
        mHangulJamoStack[1] = 0;
        mHangulJamoStack[2] = 0;
        return;
    }

    private void hangulSendKey(int newHangulChar, int hCursor) {

        if (hCursor == HCURSOR_NEW) {
            Log.i("Hangul", "HCURSOR_NEW");

            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_NEW;
        }
        else if (hCursor == HCURSOR_ADD) {
            mHCursorState = HCURSOR_ADD;
            Log.i("Hangul", "HCURSOR_ADD");
            if (mComposing.length() > 0) {
                mComposing.setLength(0);
                getCurrentInputConnection().finishComposingText();
            }

            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        }
        else if (hCursor == HCURSOR_UPDATE) {
            Log.i("Hangul", "HCURSOR_UPDATE");
            mComposing.setCharAt(0, (char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_UPDATE;
        }
        else if (hCursor == HCURSOR_APPEND) {
            Log.i("Hangul", "HCURSOR_APPEND");
            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_APPEND;
        }
        else if (hCursor == HCURSOR_NONE) {
            if (newHangulChar == -1) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -1]");
                keyDownUp(KeyEvent.KEYCODE_DEL);
                clearHangul();
            }
            else if (newHangulChar == -2) {
//              int hangulKeyIdx;
//              int cho_idx,jung_idx,jong_idx;

                Log.i("Hangul", "HCURSOR_NONE [DEL -2]");
                handleBackspace();
/*
                switch(mHangulState) {
                case H_STATE_0:
                    keyDownUp(KeyEvent.KEYCODE_DEL);
                    break;
                case H_STATE_1: // �ʼ�
//                  keyDownUp(KeyEvent.KEYCODE_DEL);
                    mComposing.setLength(0);
                    getCurrentInputConnection().commitText("", 0);
                    clearHangul();
                    mHangulState = H_STATE_0;
                    break;
                case H_STATE_2: // �ʼ�(������)
                    newHangulChar = 0x3131 + mHangulKeyStack[0];
                    hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                    mHangulKeyStack[1] = 0;
                    mHangulJamoStack[0] = mHangulKeyStack[0];
                    mHangulState = H_STATE_1; // goto �ʼ�       
                    break;
                case H_STATE_3: // �߼�(�ܸ���,������)
                    if (mHangulKeyStack[3] == 0) {
//                      keyDownUp(KeyEvent.KEYCODE_DEL);
                        mComposing.setLength(0);
                        getCurrentInputConnection().commitText("", 0);
                        clearHangul();  
                        mHangulState = H_STATE_0;
                    }
                    else {
                        mHangulKeyStack[3] = 0;
                        newHangulChar = 0x314F + (mHangulKeyStack[2] - 30);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        mHangulState = H_STATE_3; // goto �߼�                                               
                    }
                    break;
                case H_STATE_4: // �ʼ�,�߼�(�ܸ���,������)
                    if (mHangulKeyStack[3] == 0) {
                        mHangulKeyStack[2] = 0;
                        mHangulJamoStack[1] = 0;
                        newHangulChar = 0x3131 + mHangulJamoStack[0];
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulState = H_STATE_1; // goto �ʼ�                       
                    }
                    else {
                        mHangulJamoStack[1]= mHangulKeyStack[2];
                        mHangulKeyStack[3] = 0;
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                    }
                    break;
                case H_STATE_5: // �ʼ�,�߼�,����
                    mHangulJamoStack[2] = 0;
                    mHangulKeyStack[4] = 0;
                    cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                    jung_idx = mHangulJamoStack[1] - 30;
                    jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                    newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                    hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                    mHangulState = H_STATE_4;
                    break;
                case H_STATE_6:     
                    mHangulKeyStack[5] = 0;
                    mHangulJamoStack[2] = mHangulKeyStack[4];
                    cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                    jung_idx = mHangulJamoStack[1] - 30;
                    jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                    newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                    hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                    mHangulState = H_STATE_5;
                    break;
                }*/
            }
            else if (newHangulChar == -3) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -3]");
                final int length = mComposing.length();
                if (length > 1) {
                    mComposing.delete(length - 1, length);
                }
            }

        }
    }

    private void hangulQwertySendKey(int newHangulChar, int hCursor) {

        if (hCursor == HCURSOR_NEW) {
            Log.i("Hangul", "HCURSOR_NEW<" + String.format("%c", newHangulChar) + ">");

            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_NEW;
        }
        else if (hCursor == HCURSOR_ADD) {
            mHCursorState = HCURSOR_ADD;
            Log.i("Hangul", "HCURSOR_ADD<" + String.format("%c", newHangulChar) + ">");
            if (mComposing.length() > 0) {
                mComposing.setLength(0);
                getCurrentInputConnection().finishComposingText();
            }

            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        }
        else if (hCursor == HCURSOR_UPDATE) {
            Log.i("Hangul", "HCURSOR_UPDATE<" + String.format("%c", newHangulChar) + ">");
            mComposing.setCharAt(0, (char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_UPDATE;
        }
        else if (hCursor == HCURSOR_APPEND) {
            Log.i("Hangul", "HCURSOR_APPEND<" + String.format("%c", newHangulChar) + ">");
            mComposing.append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mHCursorState = HCURSOR_APPEND;
        }
        else if (hCursor == HCURSOR_NONE) {
            if (newHangulChar == -1) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -1]");
                keyDownUp(KeyEvent.KEYCODE_DEL);
                clearHangul();
            }
            else if (newHangulChar == -2) {
                int hangulKeyIdx;
                int cho_idx,jung_idx,jong_idx;

                Log.i("Hangul", "HCURSOR_NONE [DEL -2]");

                switch(mHangulState) {
                    case H_STATE_0:
                        keyDownUp(KeyEvent.KEYCODE_DEL);
                        break;
                    case H_STATE_1: // 초성
//                  keyDownUp(KeyEvent.KEYCODE_DEL);
                        mComposing.setLength(0);
                        getCurrentInputConnection().commitText("", 0);
                        clearHangul();
                        mHangulState = H_STATE_0;
                        break;
                    case H_STATE_2: // 초성(복자음)
                        newHangulChar = 0x3131 + mHangulKeyStack[0];
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulKeyStack[1] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulState = H_STATE_1; // goto 초성
                        break;
                    case H_STATE_3: // 중성(단모음,복모음)
                        if (mHangulKeyStack[3] == 0) {
//                      keyDownUp(KeyEvent.KEYCODE_DEL);
                            mComposing.setLength(0);
                            getCurrentInputConnection().commitText("", 0);
                            clearHangul();
                            mHangulState = H_STATE_0;
                        }
                        else {
                            mHangulKeyStack[3] = 0;
                            newHangulChar = 0x314F + (mHangulKeyStack[2] - 30);
                            hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulJamoStack[1] = mHangulKeyStack[2];
                            mHangulState = H_STATE_3; // goto 중성
                        }
                        break;
                    case H_STATE_4: // 초성,중성(단모음,복모음)
                        if (mHangulKeyStack[3] == 0) {
                            mHangulKeyStack[2] = 0;
                            mHangulJamoStack[1] = 0;
                            newHangulChar = 0x3131 + mHangulJamoStack[0];
                            hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulState = H_STATE_1; // goto 초성
                        }
                        else {
                            mHangulJamoStack[1]= mHangulKeyStack[2];
                            mHangulKeyStack[3] = 0;
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        }
                        break;
                    case H_STATE_5: // 초성,중성,종성
                        mHangulJamoStack[2] = 0;
                        mHangulKeyStack[4] = 0;
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulState = H_STATE_4;
                        break;
                    case H_STATE_6:
                        mHangulKeyStack[5] = 0;
                        mHangulJamoStack[2] = mHangulKeyStack[4];
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                        mHangulState = H_STATE_5;
                        break;
                }
            }
            else if (newHangulChar == -3) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -3]");
                final int length = mComposing.length();
                if (length > 1) {
                    mComposing.delete(length - 1, length);
                }
            }

        }
    }

    private int prevJaumKeyCode = 0;


    final private char ko_first_state[] = {
            1,3,4,6,7,8,10,12,13,   // 0
            16,3,4,6,7,8,10,12,13,  // 1
            1,3,4,6,7,8,10,12,13,   // 2 
            1,3,4,6,7,8,10,12,13,   // 3
            1,3,17,6,7,8,10,12,13,  // 4
            1,3,4,6,7,8,10,12,13,   // 5
            1,3,4,6,7,8,10,12,13,   // 6
            1,3,4,6,7,8,10,12,13,   // 7
            1,3,4,6,7,18,10,12,13,  // 8
            1,3,4,6,7,8,10,12,13,   // 9
            1,3,4,6,7,8,11,12,13,   // 10
            1,3,4,6,7,8,10,12,13,   // 11
            1,3,4,6,7,8,10,19,13,   // 12
            1,3,4,6,7,8,10,12,15,   // 13
            1,3,4,6,7,8,10,12,13,   // 14
            1,3,4,6,7,8,10,12,14,   // 15
            2,3,4,6,7,8,10,12,13,   // 16
            1,3,5,6,7,8,10,12,13,   // 17
            1,3,4,6,7,9,10,12,13,   // 18
            1,3,4,6,7,8,10,12,13,   // 19
    };

    final private char ko_middle_state[] = {
//          0              1                 2          3               4               5               6               7               
            23,1,21,    7,2,11,     9,1,15,     4,5,0,      0,0,0,      6,3,0,      0,0,0,      8,0,0,
//          8               9               10          11            12            13            14            15
            0,0,0,      10,0,0,     0,0,0,      14,0,0,     13,0,0,     0,0,0,      0,12,0,     0,0,0,
//          16            17              18              19              20              21              22            23
            19,20,0,    18,0,0,     0,0,0,      0,0,0,      17,16,0,    22,16,0,    0,0,0,      0,3,0
    };

    final private char ko_last_state[] = {
            1,4,7,8,16,17,19,21,22, // 0
            24,0,0,0,0,0,3,0,0,     // 1
            1,0,0,0,0,0,0,0,0,      // 2    
            0,0,0,0,0,0,31,0,0,     // 3
            0,0,0,0,0,0,0,32,5,     // 4
            0,0,0,0,0,0,0,0,33,     // 5
            0,0,0,0,0,0,0,0,0,      // 6
            0,0,25,0,0,0,0,0,0,     // 7
            9,0,44,0,0,11,12,43,0,      // 8
            36,0,0,0,0,0,0,0,0,     // 9
            0,0,0,0,0,0,0,0,0,      // 10
            0,0,0,0,0,14,0,0,0,     // 11
            0,0,0,0,0,0,39,0,0,     // 12
            0,0,45,0,0,0,0,0,0,     // 13
            0,0,0,0,0,38,0,0,0,     // 14
            0,0,0,0,0,0,0,43,0,     // 15
            0,0,0,0,0,0,0,0,0,      // 16
            0,0,0,0,0,26,18,0,0,        // 17
            0,0,0,0,0,0,41,0,0,     // 18
            0,0,0,0,0,0,20,0,0,     // 19
            0,0,0,0,0,0,19,0,0,     // 20
            0,0,0,0,0,0,0,0,0,      // 21
            0,0,0,0,0,0,0,0,23,     // 22
            0,0,0,0,0,0,0,0,42,     // 23
            2,0,0,0,0,0,0,0,0,      // 24
            0,0,28,0,0,0,0,0,0,     // 25
            0,0,0,0,0,29,0,0,0,     // 26
            0,0,0,0,0,0,0,0,0,      // 27
            0,0,7,0,0,0,0,0,0,      // 28
            0,0,0,0,0,17,0,0,0,     // 29
            0,0,0,0,0,0,0,0,0,      // 30
            0,0,0,0,0,0,3,0,0,      // 31
            0,0,0,0,0,0,0,35,0,     // 32
            0,0,0,0,0,0,0,0,34,     // 33
            0,0,0,0,0,0,0,0,5,      // 34
            0,0,0,0,0,0,0,32,0,     // 35
            37,0,0,0,0,0,0,0,0,     // 36
            9,0,0,0,0,0,0,0,0,      // 37
            0,0,0,0,0,11,0,0,0,     // 38
            0,0,0,0,0,0,12,0,0,     // 39
            0,0,0,0,0,0,0,0,0,      // 40
            0,0,0,0,0,0,18,0,0,     // 41
            0,0,0,0,0,0,0,0,22,     // 42
            0,0,0,0,0,0,0,15,0,     // 43
            0,0,13,0,0,0,0,0,0,     // 44
            0,0,44,0,0,0,0,0,0,     // 45
    };

    final private char ko_jong_m_split[] = {
            0,1, 0,2, 1,10, 0,3, 4,13,
            4,19, 0,4, 0,6, 8,1, 8,7,
            8,8, 8,10, 8,17, 8,18, 8,19,
            0,7,0,8,17,10,0,10,0,11,
            0,12,0,13,0,15,0,16,0,17,
            0,18,0,19
    };

    final private char ko_jong_l_split[] = {
            0,5,0,9,
            1,19,1,11,
            4,12,4,15,4,14,4,19,
            8,16,8,2,8,9,8,11,
            17,19,17,11,
            0,14,
            8,12,8,4,8,5
    };

    final private char jongsung_28idx[] = {
            0, 0, 1, 1, 4, 4, 4, 8, 8, 8, 8, 17, 17, 0, 8, 8, 8
    };

    final static char KO_S_0000 = 0;
    final static char KO_S_0100 = 1;
    final static char KO_S_1000 = 2;
    final static char KO_S_1100 = 3;
    final static char KO_S_1110 = 4;
    final static char KO_S_1111 = 5;

    private int prev_key = -1;
    private char ko_state_idx = KO_S_0000;
    private char ko_state_first_idx;
    private char ko_state_middle_idx;
    private char ko_state_last_idx;
    private char ko_state_next_idx;


    final private int key_idx[] =
            {0, 1, 2, 3, 4, 5, 6, 7, 8,0,1,2,};
    // ��,��,��,��,��,��,��, ��,��,��,��,��,

    /*
        final private char chosung_code[] = {
                0, 1, 3, 6, 7, 8, 16, 17, 18, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
            };
        final private char jongsung_code[] = {
                0,1,2,3,4,5,6,  // ��
                8,9,10,11,12,13,14,15,16,17, // ��
                19,20,21,22,23, // ��
                25,26,27,28,29
        };

        final private char jungsung_stack[] = {
            // 1  2 3  4  5  6  7 8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23
            // . .. ��,��,��,��,��,��,��,��,��,��,��, ��,��,��,��,��,��,��,��,��, ��
               0,0, 0, 0, 0, 0, 0, 0,0, 0, 0, 11,11,11, 0, 0,16,16,16,0, 0, 21, 0
        };
    */
    private boolean isChoKeyboard = true;

    private void handleHangul(int primaryCode, int[] keyCodes) {

        int hangulKeyIdx = -1;
        int newHangulChar;
        int cho_idx,jung_idx,jong_idx;
        int hangulChar = 0;
/*        
        if (mHangulCursorMoved == 1) {
            clearHangul();
            Log.i("Hangul", "clear Hangul at handleHangul by mHangulCursorMoved");
            mHangulCursorMoved = 0;
        }
*/
        Log.i("Hangul", "PrimaryCode[" + Integer.toString(primaryCode)+"]");

        if (primaryCode >= 0x61 && primaryCode <= 0x7A) {
            Log.i("SoftKey", "handleHangul - hancode");

            if (mHangulShiftState == 0) {
                hangulKeyIdx = e2h_map[primaryCode - 0x61];
            }
            else {
                hangulKeyIdx = e2h_map[primaryCode - 0x61 + 26];
//                Keyboard currentKeyboard = mInputView.getKeyboard();
                mHangulShiftedKeyboard.setShifted(false);
                mInputView.setKeyboard(mHangulKeyboard);
                mHangulKeyboard.setShifted(false);
                mHangulShiftState = 0;
            }
            hangulChar = 1;
        }
        else if (primaryCode >= 0x41 && primaryCode <= 0x5A) {
            hangulKeyIdx = e2h_map[primaryCode - 0x41 + 26];
            hangulChar = 1;
        }
        /*
        else  if (primaryCode >= 0x3131 && primaryCode <= 0x3163) {
            hangulKeyIdx = primaryCode - 0x3131;
            hangulChar = 1;
        }
        */
        else {
            hangulChar = 0;
        }

        if (hangulChar == 1) {

            if (hangulKeyIdx < 30 && isChoKeyboard && mPredictionOn) {

                hangulSendKey(0x3131 + hangulKeyIdx, HCURSOR_NEW);
                updateCandidates();

            }
            else {

                switch(mHangulState) {

                    case H_STATE_0: // Hangul Clear State
                        // Log.i("SoftKey", "HAN_STATE 0");
                        if (hangulKeyIdx < 30) { // if ����
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar, HCURSOR_NEW);
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else { // if ����
                            newHangulChar = 0x314F + (hangulKeyIdx - 30);
                            hangulSendKey(newHangulChar, HCURSOR_NEW);
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            mHangulState = H_STATE_3; // goto �߼�
                        }
                        break;

                    case H_STATE_1: // �ʼ�
                        // Log.i("SoftKey", "HAN_STATE 1");
                        if (hangulKeyIdx < 30) { // if ����
                            int newHangulKeyIdx = isHangulKey(0,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // if ������
                                newHangulChar = 0x3131 + newHangulKeyIdx;
                                mHangulKeyStack[1] = hangulKeyIdx;
                                mHangulJamoStack[0] = newHangulKeyIdx;
                                //                      hangulSendKey(-1);
                                hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                mHangulState = H_STATE_2; // goto �ʼ�(������)
                            }
                            else { // if ����

                                // cursor error trick start
                                newHangulChar = 0x3131 + mHangulJamoStack[0];
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x3131 + hangulKeyIdx;
                                hangulSendKey(newHangulChar, HCURSOR_ADD);
                                mHangulKeyStack[0] = hangulKeyIdx;
                                mHangulJamoStack[0] = hangulKeyIdx;
                                mHangulState = H_STATE_1; // goto �ʼ�
                            }
                        }
                        else { // if ����
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            //                  hangulSendKey(-1);
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                        }
                        break;

                    case H_STATE_2: // �ʼ�(������)
                        // Log.i("SoftKey", "HAN_STATE 2");
                        if (hangulKeyIdx < 30) { // if ����

                            // cursor error trick start
                            newHangulChar = 0x3131 + mHangulJamoStack[0];
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar, HCURSOR_ADD);
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else { // if ����
                            newHangulChar = 0x3131 + mHangulKeyStack[0];
                            mHangulKeyStack[0] = mHangulKeyStack[1];
                            mHangulJamoStack[0] = mHangulKeyStack[0];
                            mHangulKeyStack[1] = 0;
                            //                  hangulSendKey(-1);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = mHangulKeyStack[2];
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;

                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_ADD);
                            mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                        }
                        break;

                    case H_STATE_3: // �߼�(�ܸ���,������)
                        // Log.i("SoftKey", "HAN_STATE 3");
                        if (hangulKeyIdx < 30) { // ����

                            // cursor error trick start
                            newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            // trick end

                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar, HCURSOR_ADD);
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else { // ����
                            if (mHangulKeyStack[3] == 0) {
                                int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                                if (newHangulKeyIdx > 0) { // ������
                                    //                      hangulSendKey(-1);
                                    newHangulChar = 0x314F + (newHangulKeyIdx - 30);
                                    hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                    mHangulKeyStack[3] = hangulKeyIdx;
                                    mHangulJamoStack[1] = newHangulKeyIdx;
                                }
                                else { // ����

                                    // cursor error trick start
                                    newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                                    hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                    // trick end

                                    newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                    hangulSendKey(newHangulChar,HCURSOR_ADD);
                                    mHangulKeyStack[2] = hangulKeyIdx;
                                    mHangulJamoStack[1] = hangulKeyIdx;
                                }
                            }
                            else {

                                // cursor error trick start
                                newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                                hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                                mHangulKeyStack[3] = 0;
                            }
                            mHangulState = H_STATE_3;
                        }
                        break;
                    case H_STATE_4: // �ʼ�,�߼�(�ܸ���,������)
                        // Log.i("SoftKey", "HAN_STATE 4");
                        if (hangulKeyIdx < 30) { // if ����
                            mHangulKeyStack[4] = hangulKeyIdx;
                            mHangulJamoStack[2] = hangulKeyIdx;
                            //                  hangulSendKey(-1);
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            if (jong_idx == 0) { // if ���� is not valid ex, �� + ��
                                mHangulKeyStack[0] = hangulKeyIdx;
                                mHangulKeyStack[1] = 0;
                                mHangulKeyStack[2] = 0;
                                mHangulKeyStack[3] = 0;
                                mHangulKeyStack[4] = 0;
                                mHangulJamoStack[0] = hangulKeyIdx;
                                mHangulJamoStack[1] = 0;
                                mHangulJamoStack[2] = 0;
                                newHangulChar = 0x3131 + hangulKeyIdx;
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulState = H_STATE_1; // goto �ʼ�
                            }
                            else {
                                mHangulState = H_STATE_5; // goto �ʼ�,�߼�,����
                            }
                        }
                        else { // if ����
                            if (mHangulKeyStack[3] == 0) {
                                int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                                if (newHangulKeyIdx > 0) { // if ������
                                    //                      hangulSendKey(-1);
                                    //                      mHangulKeyStack[2] = newHangulKeyIdx;
                                    mHangulKeyStack[3] = hangulKeyIdx;
                                    mHangulJamoStack[1] = newHangulKeyIdx;
                                    cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                    jung_idx = mHangulJamoStack[1] - 30;
                                    jong_idx = 0;
                                    newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                    hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                    mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                                }
                                else { // if invalid ������

                                    // cursor error trick start
                                    cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                    jung_idx = mHangulJamoStack[1] - 30;
                                    jong_idx = 0;
                                    newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                    hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                    // trick end

                                    newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                    hangulSendKey(newHangulChar,HCURSOR_ADD);
                                    mHangulKeyStack[0] = 0;
                                    mHangulKeyStack[1] = 0;
                                    mHangulJamoStack[0] = 0;
                                    mHangulKeyStack[2] = hangulKeyIdx;
                                    mHangulJamoStack[1] = hangulKeyIdx;
                                    mHangulState = H_STATE_3; // goto �߼�
                                }
                            }
                            else {

                                // cursor error trick start
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = 0;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                // trick end


                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[0] = 0;
                                mHangulKeyStack[1] = 0;
                                mHangulJamoStack[0] = 0;
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                                mHangulKeyStack[3] = 0;
                                mHangulState = H_STATE_3; // goto �߼�

                            }
                        }
                        break;
                    case H_STATE_5: // �ʼ�,�߼�,����
                        // Log.i("SoftKey", "HAN_STATE 5");
                        if (hangulKeyIdx < 30) { // if ����
                            int newHangulKeyIdx = isHangulKey(4,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // if ���� == ������
                                //                      hangulSendKey(-1);
                                mHangulKeyStack[5] = hangulKeyIdx;
                                mHangulJamoStack[2] = newHangulKeyIdx;

                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                mHangulState = H_STATE_6; // goto  �ʼ�,�߼�,����(������)
                            }
                            else { // if ���� != ������

                                // cursor error trick start
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                // trick end


                                mHangulKeyStack[0] = hangulKeyIdx;
                                mHangulKeyStack[1] = 0;
                                mHangulKeyStack[2] = 0;
                                mHangulKeyStack[3] = 0;
                                mHangulKeyStack[4] = 0;
                                mHangulJamoStack[0] = hangulKeyIdx;
                                mHangulJamoStack[1] = 0;
                                mHangulJamoStack[2] = 0;
                                newHangulChar = 0x3131 + hangulKeyIdx;
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulState = H_STATE_1; // goto �ʼ�
                            }
                        }
                        else { // if ����
                            //                  hangulSendKey(-1);

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                            mHangulKeyStack[0] = mHangulKeyStack[4];
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = mHangulKeyStack[0];
                            mHangulJamoStack[1] = mHangulKeyStack[2];
                            mHangulJamoStack[2] = 0;

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_ADD);

                            // Log.i("SoftKey", "--- Goto HAN_STATE 4");
                            mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                        }
                        break;
                    case H_STATE_6: // �ʼ�,�߼�,����(������)
                        // Log.i("SoftKey", "HAN_STATE 6");
                        if (hangulKeyIdx < 30) { // if ����

                            // cursor error trick start
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = 0;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulJamoStack[2] = 0;

                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar,HCURSOR_ADD);

                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else { // if ����
                            //                  hangulSendKey(-1);
                            mHangulJamoStack[2] = mHangulKeyStack[4];

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                            mHangulKeyStack[0] = mHangulKeyStack[5];
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulKeyStack[5] = 0;
                            mHangulJamoStack[0] = mHangulKeyStack[0];
                            mHangulJamoStack[1] = mHangulKeyStack[2];
                            mHangulJamoStack[2] = 0;

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar,HCURSOR_ADD);

                            mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                        }
                        break;
                }
            }
        }
        else {
            // Log.i("Hangul", "handleHangul - No hancode");
            clearHangul();
            sendKey(primaryCode);
        }

    }

    private void handleHangulQwerty(int primaryCode, int[] keyCodes) {

        int hangulKeyIdx = -1;
        int newHangulChar;
        int cho_idx,jung_idx,jong_idx;
        int hangulChar = 0;
/*        
        if (mHangulCursorMoved == 1) {
            clearHangul();
            Log.i("Hangul", "clear Hangul at handleHangulQwerty by mHangulCursorMoved");
            mHangulCursorMoved = 0;
        }
*/
        // Log.i("Hangul", "PrimaryCode[" + Integer.toString(primaryCode)+"]"); 

        if (primaryCode >= 0x61 && primaryCode <= 0x7A) {
            //            Log.i("SoftKey", "handleHangulQwerty - hancode");

            if (mHangulShiftState == 0) {
                hangulKeyIdx = e2h_map[primaryCode - 0x61];
            }
            else {
                hangulKeyIdx = e2h_map[primaryCode - 0x61 + 26];
//                Keyboard currentKeyboard = mInputView.getKeyboard();
                mHangulQwertyShiftedKeyboard.setShifted(false);
                mInputView.setKeyboard(mHangulQwertyKeyboard);
                mHangulQwertyKeyboard.setShifted(false);
                mHangulShiftState = 0;
            }
            hangulChar = 1;
        }
        else if (primaryCode >= 0x41 && primaryCode <= 0x5A) {
            hangulKeyIdx = e2h_map[primaryCode - 0x41 + 26];
            hangulChar = 1;
        }
        /*
        else  if (primaryCode >= 0x3131 && primaryCode <= 0x3163) {
            hangulKeyIdx = primaryCode - 0x3131;
            hangulChar = 1;
        }
        */
        else {
            hangulChar = 0;
        }


        if (hangulChar == 1) {

            switch(mHangulState) {

                case H_STATE_0: // Hangul Clear State
                    // Log.i("SoftKey", "HAN_STATE 0");
                    if (hangulKeyIdx < 30) { // if 자음
                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulQwertySendKey(newHangulChar, HCURSOR_NEW);
                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulState = H_STATE_1; // goto 초성
                    }
                    else { // if 모음
                        newHangulChar = 0x314F + (hangulKeyIdx - 30);
                        hangulQwertySendKey(newHangulChar, HCURSOR_NEW);
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = hangulKeyIdx;
                        mHangulState = H_STATE_3; // goto 중성
                    }
                    break;

                case H_STATE_1: // 초성
                    // Log.i("SoftKey", "HAN_STATE 1");
                    if (hangulKeyIdx < 30) { // if 자음
                        int newHangulKeyIdx = isHangulKey(0,hangulKeyIdx);
                        if (newHangulKeyIdx > 0) { // if 복자음
                            newHangulChar = 0x3131 + newHangulKeyIdx;
                            mHangulKeyStack[1] = hangulKeyIdx;
                            mHangulJamoStack[0] = newHangulKeyIdx;
//                      hangulQwertySendKey(-1);
                            hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulState = H_STATE_2; // goto 초성(복자음)
                        }
                        else { // if 자음

                            // cursor error trick start
                            newHangulChar = 0x3131 + mHangulJamoStack[0];
                            hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end

                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulQwertySendKey(newHangulChar, HCURSOR_ADD);
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulState = H_STATE_1; // goto 초성
                        }
                    }
                    else { // if 모음
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = hangulKeyIdx;
//                  hangulQwertySendKey(-1);
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulState = H_STATE_4; // goto 초성,중성
                    }
                    break;

                case H_STATE_2: // 초성(복자음)
                    // Log.i("SoftKey", "HAN_STATE 2");
                    if (hangulKeyIdx < 30) { // if 자음

                        // cursor error trick start
                        newHangulChar = 0x3131 + mHangulJamoStack[0];
                        hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                        // trick end


                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulQwertySendKey(newHangulChar, HCURSOR_ADD);
                        mHangulState = H_STATE_1; // goto 초성
                    }
                    else { // if 모음
                        newHangulChar = 0x3131 + mHangulKeyStack[0];
                        mHangulKeyStack[0] = mHangulKeyStack[1];
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulKeyStack[1] = 0;
//                  hangulQwertySendKey(-1);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;

                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_ADD);
                        mHangulState = H_STATE_4; // goto 초성,중성
                    }
                    break;

                case H_STATE_3: // 중성(단모음,복모음)
                    // Log.i("SoftKey", "HAN_STATE 3");
                    if (hangulKeyIdx < 30) { // 자음

                        // cursor error trick start
                        newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        // trick end

                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulQwertySendKey(newHangulChar, HCURSOR_ADD);
                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        mHangulState = H_STATE_1; // goto 초성
                    }
                    else { // 모음
                        if (mHangulKeyStack[3] == 0) {
                            int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // 복모음
                                //                      hangulQwertySendKey(-1);
                                newHangulChar = 0x314F + (newHangulKeyIdx - 30);
                                hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                                mHangulKeyStack[3] = hangulKeyIdx;
                                mHangulJamoStack[1] = newHangulKeyIdx;
                            }
                            else { // 모음

                                // cursor error trick start
                                newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                                hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                            }
                        }
                        else {

                            // cursor error trick start
                            newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                            hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                            // trick end

                            newHangulChar = 0x314F + (hangulKeyIdx - 30);
                            hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                        }
                        mHangulState = H_STATE_3;
                    }
                    break;
                case H_STATE_4: // 초성,중성(단모음,복모음)
                    // Log.i("SoftKey", "HAN_STATE 4");
                    if (hangulKeyIdx < 30) { // if 자음
                        mHangulKeyStack[4] = hangulKeyIdx;
                        mHangulJamoStack[2] = hangulKeyIdx;
//                  hangulQwertySendKey(-1);
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);
                        if (jong_idx == 0) { // if 종성 is not valid ex, 라 + ㅉ
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = 0;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulJamoStack[2] = 0;
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                            mHangulState = H_STATE_1; // goto 초성
                        }
                        else {
                            mHangulState = H_STATE_5; // goto 초성,중성,종성
                        }
                    }
                    else { // if 모음
                        if (mHangulKeyStack[3] == 0) {
                            int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // if 복모음
                                //                      hangulQwertySendKey(-1);
                                //                      mHangulKeyStack[2] = newHangulKeyIdx;
                                mHangulKeyStack[3] = hangulKeyIdx;
                                mHangulJamoStack[1] = newHangulKeyIdx;
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = 0;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                                mHangulState = H_STATE_4; // goto 초성,중성
                            }
                            else { // if invalid 복모음

                                // cursor error trick start
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = 0;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[0] = 0;
                                mHangulKeyStack[1] = 0;
                                mHangulJamoStack[0] = 0;
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                                mHangulState = H_STATE_3; // goto 중성
                            }
                        }
                        else {

                            // cursor error trick start
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            newHangulChar = 0x314F + (hangulKeyIdx - 30);
                            hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                            mHangulKeyStack[0] = 0;
                            mHangulKeyStack[1] = 0;
                            mHangulJamoStack[0] = 0;
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                            mHangulState = H_STATE_3; // goto 중성

                        }
                    }
                    break;
                case H_STATE_5: // 초성,중성,종성
                    // Log.i("SoftKey", "HAN_STATE 5");
                    if (hangulKeyIdx < 30) { // if 자음
                        int newHangulKeyIdx = isHangulKey(4,hangulKeyIdx);
                        if (newHangulKeyIdx > 0) { // if 종성 == 복자음
//                      hangulQwertySendKey(-1);
                            mHangulKeyStack[5] = hangulKeyIdx;
                            mHangulJamoStack[2] = newHangulKeyIdx;

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                            mHangulState = H_STATE_6; // goto  초성,중성,종성(복자음)
                        }
                        else { // if 종성 != 복자음

                            // cursor error trick start
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = 0;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulJamoStack[2] = 0;
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulQwertySendKey(newHangulChar,HCURSOR_ADD);
                            mHangulState = H_STATE_1; // goto 초성
                        }
                    }
                    else { // if 모음
//                  hangulQwertySendKey(-1);

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[0] = mHangulKeyStack[4];
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        mHangulJamoStack[2] = 0;

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_ADD);

                        // Log.i("SoftKey", "--- Goto HAN_STATE 4");
                        mHangulState = H_STATE_4; // goto 초성,중성
                    }
                    break;
                case H_STATE_6: // 초성,중성,종성(복자음)
                    // Log.i("SoftKey", "HAN_STATE 6");
                    if (hangulKeyIdx < 30) { // if 자음

                        // cursor error trick start
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar,HCURSOR_UPDATE);
                        // trick end


                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = 0;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        mHangulJamoStack[2] = 0;

                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulQwertySendKey(newHangulChar,HCURSOR_ADD);

                        mHangulState = H_STATE_1; // goto 초성
                    }
                    else { // if 모음
//                  hangulQwertySendKey(-1);
                        mHangulJamoStack[2] = mHangulKeyStack[4];

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[0] = mHangulKeyStack[5];
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulKeyStack[5] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        mHangulJamoStack[2] = 0;

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulQwertySendKey(newHangulChar,HCURSOR_ADD);

                        mHangulState = H_STATE_4; // goto 초성,중성
                    }
                    break;
            }
        }
        else {
            // Log.i("Hangul", "handleHangulQwerty - No hancode");
            clearHangul();
            sendKey(primaryCode);
        }

    }
// Hangul Code End    

    private void handleCharacter(int primaryCode, int[] keyCodes) {

        Log.d ("Movie", "handleCharacter");
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            sendKeyChar((char)primaryCode);
            /*
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
            */
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (!mCompletionOn && mCandidateView != null && index >= 0
                && index < mCandidateView.getSuggestionLength()) {
            Log.d("Movie", "Success idx:" + index);
            String s = mCandidateView.getCompletionInfo(index);
            InputConnection ic = getCurrentInputConnection();
            ic.commitText(s, s.length());
            ic.finishComposingText();
            mCandidateView.clear();

            updateShiftKeyState(getCurrentInputEditorInfo());

            dbHandler.onCandidateSelected (s, previous);

        } else if (mComposing.length() > 0) {
            Log.d("Movie", "Fail    idx:" + index + " sugLen:" + mCandidateView.getSuggestionLength());
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    public void swipeRight() {

        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();

    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }
}
