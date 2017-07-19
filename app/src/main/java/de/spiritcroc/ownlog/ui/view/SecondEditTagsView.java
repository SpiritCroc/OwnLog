package de.spiritcroc.ownlog.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import de.spiritcroc.ownlog.R;

/**
 * TODO: deprecate this class by unifying it with its superclass and putting all required values into attrs
 */
public class SecondEditTagsView extends EditTagsView {

    public SecondEditTagsView(Context context) {
        this(context, null);
    }

    public SecondEditTagsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SecondEditTagsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected int getAddTagButtonId() {
        return R.id.add_tag_button_1;
    }

    protected int getEditTagMsgId() {
        return R.id.tag_msg_1;
    }
}
