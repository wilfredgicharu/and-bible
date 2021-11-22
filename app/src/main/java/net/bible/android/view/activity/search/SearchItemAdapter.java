/*
 * Copyright (c) 2020 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package net.bible.android.view.activity.search;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TwoLineListItem;

import net.bible.android.control.search.SearchControl;

import org.crosswire.jsword.passage.Key;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * nice example here: http://shri.blog.kraya.co.uk/2010/04/19/android-multi-line-select-list/
 * 
 * @author Martin Denham [mjdenham at gmail dot com]
 */
public class SearchItemAdapter extends ArrayAdapter<Key> {

	private int resource;
	private SearchControl searchControl;

	public SearchItemAdapter(Context _context, int _resource, List<Key> _items, SearchControl searchControl) {
		super(_context, _resource, _items);
		this.resource = _resource;
		this.searchControl = searchControl;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Key item = getItem(position);

		// Pick up the TwoLineListItem defined in the xml file
		TwoLineListItem view;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (TwoLineListItem) inflater.inflate(resource, parent, false);
		} else {
			view = (TwoLineListItem) convertView;
		}

		// Set value for the first text field
		if (view.getText1() != null) {
			String key = item.getName();
			view.getText1().setText(key);
		}

		// set value for the second text field
		if (view.getText2() != null) {
			String verseText = searchControl.getSearchResultVerseText(item);
			SpannableString verseTextHtml = highlightSearchText(verseText, SearchControl.originalSearchString);
			view.getText2().setText(verseTextHtml);
		}

		return view;
	}
	private SpannableString highlightSearchText(String text, String searchTerms) {
		SpannableString spannableText = new SpannableString(text);
		try {
			// Split on the space characters that are not enclosed in double quotes
			String[] splitSearchArray = searchTerms.split("\\s+(?=(?:\"(?:\\\\\"|[^\"])+\"|[^\"])+$)");
			for (String searchWord : splitSearchArray) {
				searchWord = searchWord.replace("\"", "");  // Remove quotes which indicate phrase searches
				searchWord = searchWord.replace("+", "");	// Remove + which indicates AND searches
				searchWord = searchWord.replace("?", "\\p{L}");  // Handles any letter from any language
				if (searchWord.length() > 0) {
					if (Objects.equals(searchWord.substring(searchWord.length() - 1), "*")) {
						searchWord = searchWord.replace("*", "");
					} else {
						searchWord = searchWord.replace("*", "\b");  // Match on a word boundary
					}
					Matcher m = Pattern.compile(searchWord, Pattern.CASE_INSENSITIVE).matcher(spannableText);
					while (m.find()) {
						spannableText.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
		}
		catch (Exception e) {
			Log.w("SEARCH", e.getMessage());
		}
		finally {
			return spannableText;
		}
	}
}
