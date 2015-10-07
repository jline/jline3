/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

/**
 * List of all operations.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.6
 */
public class Operation {

    public static final String CALLBACK_INIT = "callback-init";
    public static final String CALLBACK_FINISH = "callback-finish";
    public static final String CALLBACK_KEYMAP = "callback-keymap";

    public static final String ACCEPT_LINE = "accept-line";
    public static final String ARGUMENT_BASE = "argument-base";
    public static final String BACKWARD_CHAR = "backward-char";
    public static final String BACKWARD_DELETE_CHAR = "backward-delete-char";
    public static final String BACKWARD_DELETE_WORD = "backward-delete-word";
    public static final String BACKWARD_KILL_LINE = "backward-kill-line";
    public static final String BACKWARD_KILL_WORD = "backward-kill-word";
    public static final String BACKWARD_WORD = "backward-word";
    public static final String BEEP = "beep";
    public static final String BEGINNING_OF_BUFFER_OR_HISTORY = "beginning-of-buffer-or-history";
    public static final String BEGINNING_OF_HISTORY = "beginning-of-history";
    public static final String BEGINNING_OF_LINE = "beginning-of-line";
    public static final String BEGINNING_OF_LINE_HIST = "beginning-of-line-hist";
    public static final String CAPITALIZE_WORD = "capitalize-word";
    public static final String CHARACTER_SEARCH = "character-search";
    public static final String CHARACTER_SEARCH_BACKWARD = "character-search-backward";
    public static final String CLEAR_SCREEN = "clear-screen";
    public static final String COMPLETE_PREFIX = "complete-prefix";
    public static final String COMPLETE_WORD = "complete-word";
    public static final String COPY_PREV_WORD = "copy-prev-word";
    public static final String COPY_REGION_AS_KILL = "copy-region-as-kill";
    public static final String DELETE_CHAR = "delete-char";
    public static final String DELETE_CHAR_OR_LIST = "delete-char-or-list";
    public static final String DELETE_WORD = "delete-word";
    public static final String DIGIT_ARGUMENT = "digit-argument";
    public static final String DO_LOWERCASE_VERSION = "do-lowercase-version";
    public static final String DOWN_CASE_WORD = "down-case-word";
    public static final String DOWN_HISTORY = "down-history";
    public static final String DOWN_LINE = "down-line";
    public static final String DOWN_LINE_OR_HISTORY = "down-line-or-history";
    public static final String DOWN_LINE_OR_SEARCH = "down-line-or-search";
    public static final String EMACS_BACKWARD_WORD = "emacs-backward-word";
    public static final String EMACS_EDITING_MODE = "emacs-editing-mode";
    public static final String EMACS_FORWARD_WORD = "emacs-forward-word";
    public static final String END_OF_BUFFER_OR_HISTORY = "end-of-buffer-or-history";
    public static final String END_OF_HISTORY = "end-of-history";
    public static final String END_OF_LINE = "end-of-line";
    public static final String END_OF_LINE_HIST = "end-of-line-hist";
    public static final String EXCHANGE_POINT_AND_MARK = "exchange-point-and-mark";
    public static final String FORWARD_CHAR = "forward-char";
    public static final String FORWARD_WORD = "forward-word";
    public static final String HISTORY_INCREMENTAL_SEARCH_BACKWARD = "history-incremental-search-backward";
    public static final String HISTORY_INCREMENTAL_SEARCH_FORWARD = "history-incremental-search-forward";
    public static final String HISTORY_SEARCH_BACKWARD = "history-search-backward";
    public static final String HISTORY_SEARCH_FORWARD = "history-search-forward";
    public static final String INSERT_CLOSE_CURLY = "insert-close-curly";
    public static final String INSERT_CLOSE_PAREN = "insert-close-paren";
    public static final String INSERT_CLOSE_SQUARE = "insert-close-square";
    public static final String INSERT_COMMENT = "insert-comment";
    public static final String KILL_BUFFER = "kill-buffer";
    public static final String KILL_LINE = "kill-line";
    public static final String KILL_REGION = "kill-region";
    public static final String KILL_WHOLE_LINE = "kill-whole-line";
    public static final String KILL_WORD = "kill-word";
    public static final String LIST_CHOICES = "list-choices";
    public static final String MAGIC_SPACE = "magic-space";
    public static final String MENU_COMPLETE = "menu-complete";
    public static final String NEG_ARGUMENT = "neg-argument";
    public static final String OVERWRITE_MODE = "overwrite-mode";
    public static final String PUT_REPLACE_SELECTION = "put-replace-selection";
    public static final String QUOTED_INSERT = "quoted-insert";
    public static final String REDISPLAY = "redisplay";
    public static final String REVERSE_MENU_COMPLETE = "reverse-menu-complete";
    public static final String SELF_INSERT = "self-insert";
    public static final String SELF_INSERT_UNMETA = "self-insert-unmeta";
    public static final String SEND_BREAK = "abort";
    public static final String SET_MARK_COMMAND = "set-mark-command";
    public static final String TRANSPOSE_CHARS = "transpose-chars";
    public static final String TRANSPOSE_WORDS = "transpose-words";
    public static final String UNDEFINED_KEY = "undefined-key";
    public static final String UNDO = "undo";
    public static final String UNIVERSAL_ARGUMENT = "universal-argument";
    public static final String UP_CASE_WORD = "up-case-word";
    public static final String UP_HISTORY = "up-history";
    public static final String UP_LINE = "up-line";
    public static final String UP_LINE_OR_HISTORY = "up-line-or-history";
    public static final String UP_LINE_OR_SEARCH = "up-line-or-search";
    public static final String VI_ADD_EOL = "vi-add-eol";
    public static final String VI_ADD_NEXT = "vi-add-next";
    public static final String VI_BACKWARD_BLANK_WORD = "vi-backward-blank-word";
    public static final String VI_BACKWARD_BLANK_WORD_END = "vi-backward-blank-word-end";
    public static final String VI_BACKWARD_CHAR = "vi-backward-char";
    public static final String VI_BACKWARD_DELETE_CHAR = "vi-backward-delete-char";
    public static final String VI_BACKWARD_KILL_WORD = "vi-backward-kill-word";
    public static final String VI_BACKWARD_WORD = "vi-backward-word";
    public static final String VI_BACKWARD_WORD_END = "vi-backward-word-end";
    public static final String VI_BEGINNING_OF_LINE = "vi-beginning-of-line";
    public static final String VI_CHANGE = "vi-change-to";
    public static final String VI_CHANGE_EOL = "vi-change-eol";
    public static final String VI_CHANGE_WHOLE_LINE = "vi-change-whole-line";
    public static final String VI_CMD_MODE = "vi-cmd-mode";
    public static final String VI_DELETE = "vi-delete";
    public static final String VI_DELETE_CHAR = "vi-delete-char";
    public static final String VI_DIGIT_OR_BEGINNING_OF_LINE = "vi-digit-or-beginning-of-line";
    public static final String VI_DOWN_LINE_OR_HISTORY = "vi-down-line-or-history";
    public static final String VI_END_OF_LINE = "vi-end-of-line";
    public static final String VI_FETCH_HISTORY = "vi-fetch-history";
    public static final String VI_FIND_NEXT_CHAR = "vi-find-next-char";
    public static final String VI_FIND_NEXT_CHAR_SKIP = "vi-find-next-char-skip";
    public static final String VI_FIND_PREV_CHAR = "vi-find-prev-char";
    public static final String VI_FIND_PREV_CHAR_SKIP = "vi-find-prev-char-skip";
    public static final String VI_FIRST_NON_BLANK = "vi-first-non-blank";
    public static final String VI_FORWARD_BLANK_WORD = "vi-forward-blank-word";
    public static final String VI_FORWARD_BLANK_WORD_END = "vi-forward-blank-word-end";
    public static final String VI_FORWARD_CHAR = "vi-forward-char";
    public static final String VI_FORWARD_WORD = "vi-forward-word";
    public static final String VI_FORWARD_WORD_END = "vi-forward-word-end";
    public static final String VI_GOTO_COLUMN = "vi-goto-column";
    public static final String VI_HISTORY_SEARCH_BACKWARD = "vi-history-search-backward";
    public static final String VI_HISTORY_SEARCH_FORWARD = "vi-history-search-forward";
    public static final String VI_INSERT = "vi-insert";
    public static final String VI_INSERT_BOL = "vi-insert-bol";
    public static final String VI_INSERT_COMMENT = "vi-insert-comment";
    public static final String VI_KILL_EOL = "vi-kill-eol";
    public static final String VI_KILL_LINE = "vi-kill-line";
    public static final String VI_MATCH_BRACKET = "vi-match-bracket";
    public static final String VI_OPER_SWAP_CASE = "vi-oper-swap-case";
    public static final String VI_PUT_AFTER = "vi-put-after";
    public static final String VI_QUOTED_INSERT = "vi-quoted-insert";
    public static final String VI_REPEAT_CHANGE = "vi-repeat-change";
    public static final String VI_REPEAT_FIND = "vi-repeat-find";
    public static final String VI_REPEAT_SEARCH = "vi-repeat-search";
    public static final String VI_REPLACE = "vi-replace";
    public static final String VI_REPLACE_CHARS = "vi-replace-chars";
    public static final String VI_REV_REPEAT_FIND = "vi-rev-repeat-find";
    public static final String VI_REV_REPEAT_SEARCH = "vi-rev-repeat-search";
    public static final String VI_SUBSTITUTE = "vi-substitute";
    public static final String VI_SWAP_CASE = "vi-swap-case";
    public static final String VI_UP_LINE_OR_HISTORY = "vi-up-line-or-history";
    public static final String VI_YANK = "vi-yank";
    public static final String VISUAL_LINE_MODE = "visual-line-mode";
    public static final String VISUAL_MODE = "visual-mode";
    public static final String WHAT_CURSOR_POSITION = "what-cursor-position";
    public static final String YANK = "yank";
    public static final String YANK_POP = "yank-pop";

}
