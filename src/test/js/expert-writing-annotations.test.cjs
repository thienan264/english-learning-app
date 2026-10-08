const {test} = require('node:test');
const assert = require('node:assert/strict');
const {segments, normalizeText, popupPosition} = require('../../main/resources/static/js/expert-writing-annotations.js');

test('only the selected occurrence of a repeated sentence is marked', () => {
    const text = 'He go home. He go home.';
    const note = {start:12, end:23, original:'He go home.', correction:'He goes home.'};
    const result = segments(text,[note]);
    assert.equal(result.map(s => s.text).join(''),text);
    assert.equal(result[0].text,'He go home. ');
    assert.equal(result.filter(s => s.note).length,1);
    assert.equal(result[1].note,note);
});
test('Unicode and multiline text retain exact UTF-16 offsets and original content', () => {
    const text = '😀\nI is happy.\nEnd.';
    const start = text.indexOf('I is');
    const result = segments(text,[{start,end:start+4,original:'I is',correction:'I am'}]);
    assert.equal(result.map(s => s.text).join(''),text);
    assert.equal(result[0].text,'😀\n');
});
test('overlapping, forged, and out of bounds marks are rejected', () => {
    const note={start:0,end:3,original:'one'};
    assert.throws(() => segments('one two',[note,note]));
    assert.throws(() => segments('one two',[{...note,original:'two'}]));
    assert.throws(() => segments('one two',[{...note,end:500}]));
    assert.throws(() => segments('one two',[{...note,start:-1}]));
});
test('HTML-like essay text stays text rather than generated markup', () => {
    const text='<img src=x onerror=alert(1)> bad';
    const start=text.indexOf('bad');
    const result=segments(text,[{start,end:start+3,original:'bad'}]);
    assert.equal(result.map(s => s.text).join(''),text);
    assert.equal(result[0].text,'<img src=x onerror=alert(1)> ');
});

test('stored CRLF and CR paragraphs use browser LF offsets for later marks', () => {
    const raw='First paragraph.\r\n\r\nHe go home.\rAnother paragraph.';
    const text=normalizeText(raw);
    assert.equal(text,'First paragraph.\n\nHe go home.\nAnother paragraph.');
    const start=text.indexOf('He go home.');
    const result=segments(text,[{start,end:start+11,original:'He go home.'}]);
    assert.equal(result.map(part=>part.text).join(''),text);
    assert.equal(result[1].text,'He go home.');
});

test('inline editor stays beside selection and inside desktop viewport', () => {
    assert.deepEqual(popupPosition({left:200,top:100,bottom:125},{width:1200,height:900},{width:420,height:300}),{left:200,top:133});
    assert.deepEqual(popupPosition({left:1100,top:700,bottom:725},{width:1200,height:900},{width:420,height:300}),{left:768,top:392});
});
test('inline editor fits narrow screens and tall selections without leaving viewport', () => {
    assert.deepEqual(popupPosition({left:250,top:100,bottom:120},{width:360,height:600},{width:336,height:450}),{left:12,top:128});
    assert.deepEqual(popupPosition({left:-20,top:20,bottom:400},{width:360,height:600},{width:336,height:576}),{left:12,top:12});
});
