const test=require('node:test');const assert=require('node:assert/strict');const vm=require('node:vm');const fs=require('node:fs');
class Element{constructor(){this.children=[];this.content='';this.handlers={};}set textContent(v){this.content=v;}get textContent(){return this.content+this.children.map(c=>c.textContent).join(' ');}append(...children){this.children.push(...children);}addEventListener(name,fn){this.handlers[name]=fn;}focus(){}replaceChildren(){this.children=[];}remove(){}}
test('Returning to chat restores course cards with separated readable prices',async()=>{
 const ids={};['chat-form','chat-input','chat-messages','chat-send','chat-clear','chat-status'].forEach(id=>ids[id]=new Element());
 const document={getElementById:id=>ids[id],querySelector:()=>({content:''}),querySelectorAll:()=>[],createElement:()=>new Element()};
 const history=[{role:'assistant',message:'Các khóa đang ưu đãi',actions:[{title:'Khóa học',url:'/courses/8',reason:'Advanced',course:{originalPrice:500000,currentPrice:350000,discountActive:true,discountPercent:30,discountEndsAt:'2026-10-09T17:44:00',enrolled:false,accessible:false}}]}];
 const context={document,Intl,fetch:async()=>({ok:true,json:async()=>history})};vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/learning-chat.js','utf8'),context);
 await new Promise(resolve=>setImmediate(resolve));
 const bubble=ids['chat-messages'].children[0];assert.match(bubble.textContent,/350[.]000/);assert.match(bubble.textContent,/500[.]000/);assert.match(bubble.textContent,/Giảm 30%/);assert.match(bubble.textContent,/17:44 ngày 09\/10\/2026/);
 const card=bubble.children[0].children[0];assert.equal(card.href,'/courses/8');const prices=card.children.find(c=>c.className==='chat-course-prices');assert.equal(prices.children.length,3);assert.equal(prices.children[1].className,undefined);
});
