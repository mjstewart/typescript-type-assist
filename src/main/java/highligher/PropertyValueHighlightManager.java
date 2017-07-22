package highligher;

import com.intellij.openapi.editor.markup.RangeHighlighter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Unfortunately cannot use HighlightManager.getInstance(project).addRangeHighlight since it removes ALL highlighted
 * properties on key input whereas the desired behaviour is to have fine grained control to remove the highlight
 * for each individual property when modified to provide nicer UI feedback for inputting all expected values.
 *
 * <p>The behaviour of {@code RangeHighlighter} is interesting, when the highlight is removed, the caret positions
 * of all other highlights shift slightly. Previously I tried to rely on using the fixed {@code TextRange} to identify
 * the highlighted region and used a hash map to map each offset to its corresponding {@code RangeHighlighter}.
 * However this was fragile since offset positions always kept moving so sometimes the highlight
 * was never removed since the {@code DocumentEvent} offset position was slightly out of range even though it
 * was correct before any editing.</p>
 *
 * <p>Instead a {@code LinkedList} of {@code RangeHighlighter}s is searched until the {@code RangeHighlighter} which
 * has its start and end offset falling within the bounds of the offset provided by the {@code DocumentEvent} fired
 * by the {@code DocumentListener}. {@code LinkedList} is used to provide faster deletions as its easier to tell when
 * there are no more highlights remaining through an empty list.
 *
 * Created by matt on 30-May-17.
 */
public class PropertyValueHighlightManager {
    private final List<RangeHighlighter> rangeHighlighterList;

    public PropertyValueHighlightManager() {
        rangeHighlighterList = new LinkedList<>();
    }

    /**
     * Adds the supplied {@code rangeHighlighter}.
     *
     * @param rangeHighlighter The {@code RangeHighlighter} to add.
     */
    public void add(RangeHighlighter rangeHighlighter) {
        rangeHighlighterList.add(rangeHighlighter);
    }

    /**
     * If the supplied {@code offset} falls within the start/end offset bounds of any existing {@code RangeHighlighter},
     * the corresponding {@code RangeHighlighter} is returned, otherwise an empty {@code Optional}.
     *
     * @param offset The offset used to location the corresponding {@code RangeHighlighter}.
     * @return {@code Optional} containing the find {@code RangeHighlighter} otherwise empty.
     */
    public Optional<RangeHighlighter> get(int offset) {
        return rangeHighlighterList.stream()
                .filter(rangeHighlighter -> isWithinBounds(offset, rangeHighlighter))
                .findAny();
    }

    /**
     * Removes the supplied {@code RangeHighlighter}
     * <p>{@code RangeHighlighter} doesn't implement equals or hashCode which is needed for removal, however it
     * doesn't matter since the object reference is fine to use in this case.</p>
     *
     * @param rangeHighlighter The {@code RangeHighlighter} to remove.
     * @return {@code true} if removed otherwise {@code false}.
     */
    public boolean remove(RangeHighlighter rangeHighlighter) {
        return rangeHighlighterList.remove(rangeHighlighter);
    }

    public boolean isEmpty() {
        return rangeHighlighterList.size() == 0;
    }

    /**
     * Only go searching for a {@code RangeHighlighter} if the internal list is not empty and the offset falls within
     * the starting and ending offset of the triggered object.
     *
     * <p>The main idea is to make removing highlights as efficient as possible. This is important since if there are
     * still remaining highlights and the user types elsewhere in the {@code Document}, the associated
     * {@code DocumentListener} invoking this method will be called each key press.</p>
     *
     * <p>The flow is to call this method first then run {@link #get} if this returns {@code true}</p>
     *
     * @param offset The offset to check if it falls within the scope of the triggered object.
     * @return {@code true} if allowed otherwise {@code false}.
     */
    public boolean isAllowedToRun(int offset) {
        if (isEmpty()) {
            return false;
        }

        RangeHighlighter first = rangeHighlighterList.get(0);
        RangeHighlighter last = rangeHighlighterList.get(rangeHighlighterList.size() - 1);
        return isWithinBounds(offset, first.getStartOffset(), last.getEndOffset());
    }

    private boolean isWithinBounds(int offset, int startOffset, int endOffset) {
        return offset >= startOffset && offset <= endOffset;
    }

    private boolean isWithinBounds(int offset, RangeHighlighter rangeHighlighter) {
        return isWithinBounds(offset, rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset());
    }

    public int size() {
        return rangeHighlighterList.size();
    }
}
