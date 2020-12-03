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
 */

import {reactive, watch} from "@vue/runtime-core";
import {sortBy, uniqWith} from "lodash";
import {findNodeAtOffset, rangesOverlap} from "@/utils";
import highlightRange from "dom-highlight-range";
import {computed} from "@vue/reactivity";

export function useGlobalBookmarks() {
    const bookmarkLabels = reactive(new Map());
    const bookmarks = reactive(new Map());
    let count = 1;

    function updateBookmarkLabels(inputData) {
        for(const v of inputData) {
            bookmarkLabels.set(v.id || -(count++), v.style)
        }
    }

    function updateBookmarks(inputData) {
        for(const v of inputData) {
            bookmarks.set(v.id, v)
        }
    }

    window.bibleViewDebug.bookmarks = bookmarks;
    window.bibleViewDebug.bookmarkLabels = bookmarkLabels;

    return {bookmarkLabels, bookmarks, updateBookmarkLabels, updateBookmarks}
}

export function useBookmarks(props, {bookmarks, bookmarkLabels}, book) {
    function showBookmarkForWholeVerse(bookmark) {
        return bookmark.elementRange === null || bookmark.book !== book
    }

    const noOrdinalNeeded = (b) => b.ordinalRange === null && props.ordinalRange === null
    const checkOrdinal = (b) =>
        b.ordinalRange !== null && props.ordinalRange !== null
        && rangesOverlap(b.ordinalRange, props.ordinalRange, true);

    const fragmentBookmarks = computed(() => {
        return Array.from(bookmarks.values()).filter(b => noOrdinalNeeded(b) || checkOrdinal(b));
    });

    const bookmarksForWholeVerse = computed(() => {
        return fragmentBookmarks.value.filter(b => showBookmarkForWholeVerse(b));
    });

    const accurateBookmarks = computed(() => {
        return fragmentBookmarks.value.filter(b => !showBookmarkForWholeVerse(b));
    });

    const styleRanges = computed(() => {
        let splitPoints = [];
        const bookmarks = accurateBookmarks.value;

        for(const b of bookmarks) {
            splitPoints.push(b.elementRange[0])
            splitPoints.push(b.elementRange[1])
        }
        splitPoints = uniqWith(
            sortBy(splitPoints, [v => v[0], v => v[1]]),
            (v1, v2) => v1[0] === v2[0] && v1[1] === v2[1]
        );

        const styleRanges = [];

        for(let i = 0; i < splitPoints.length-1; i++) {
            const elementRange = [splitPoints[i], splitPoints[i+1]];
            const labels = new Set();
            const bookmarksSet = new Set();

            bookmarks
                .filter( b => rangesOverlap(b.elementRange, elementRange))
                .forEach(b => {
                    bookmarksSet.add(b.id);
                    b.labels.forEach(l => labels.add(l))
                });

            styleRanges.push({
                elementRange,
                labels,
                bookmarks: bookmarksSet,
            });
        }
        return styleRanges;
    })

    function styleForLabelIds(bookmarkLabelIds) {
        return styleForLabels(Array.from(bookmarkLabelIds).map(v => bookmarkLabels.get(v)));
    }

    function styleForLabels(bookmarkLabels) {
        let colors = [];
        for(const s of bookmarkLabels) {
            const c = `rgba(${s.color[0]}, ${s.color[1]}, ${s.color[2]}, 15%)`
            colors.push(c);
        }
        if(colors.length === 1) {
            colors.push(colors[0]);
        }
        const span = 100/colors.length;
        const colorStr = colors.map((v, idx) => {
            let percent;
            if (idx === 0) {
                percent = `${span}%`
            } else if (idx === colors.length - 1) {
                percent = `${span * (colors.length - 1)}%`
            } else {
                percent = `${span * idx}% ${span * (idx + 1)}%`
            }
            return `${v} ${percent}`;
        }).join(", ")

        return `background-image: linear-gradient(to bottom, ${colorStr})`;
    }

    const undoHighlights = [];

    function highlightStyleRange(styleRange) {
        const [[startCount, startOff], [endCount, endOff]] = styleRange.elementRange;
        const firstElem = document.querySelector(`.frag-${props.fragmentKey} [data-element-count="${startCount}"]`);
        const secondElem = document.querySelector(`.frag-${props.fragmentKey} [data-element-count="${endCount}"]`);
        console.log("styleRange", styleRange, props.fragmentKey, startCount, endCount);
        const [first, startOff1] = findNodeAtOffset(firstElem, startOff);
        const [second, endOff1] = findNodeAtOffset(secondElem, endOff);
        const range = new Range();
        range.setStart(first, startOff1);
        range.setEnd(second, endOff1);
        const style = styleForLabelIds(styleRange.labels)
        const undo = highlightRange(range, 'span', { style });
        undoHighlights.push(undo);
    }

    watch(styleRanges, (newValue) => {
        undoHighlights.forEach(v => {
            console.log("Running undo", v);
            v()
        })
        undoHighlights.splice(0);
        for(const s of newValue) {
            highlightStyleRange(s);
        }
    });

    return {bookmarksForWholeVerse, styleForLabels}
}
