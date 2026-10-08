(() => {
'use strict';
const form=document.getElementById('chat-form'), input=document.getElementById('chat-input'), messages=document.getElementById('chat-messages'), send=document.getElementById('chat-send'), clear=document.getElementById('chat-clear'), status=document.getElementById('chat-status');
let busy=true;
const csrf=document.querySelector('meta[name="csrf-token"]').content, csrfHeader=document.querySelector('meta[name="csrf-header"]').content;
function controls(value){busy=value;input.readOnly=value;send.disabled=value;clear.disabled=value;document.querySelectorAll('#chat-suggestions button').forEach(b=>b.disabled=value);}
function append(role,text,actions=[]){const el=document.createElement('div');el.className='chat-message '+(role==='user'?'user':'assistant');el.textContent=text;
 if(actions.length){const links=document.createElement('div');links.className='chat-actions';for(const action of actions){if(!/^\/(lessons\/\d+|courses\/\d+|profile\/learning-summary)$/.test(action.url))continue;const a=document.createElement('a');a.href=action.url;a.textContent=action.title;const small=document.createElement('small');small.textContent=action.reason;a.append(small);
 if(action.course){const course=action.course;const prices=document.createElement('div');prices.className='chat-course-prices';
  const money=value=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(value);
  if(course.discountActive && course.originalPrice!=null){const original=document.createElement('del');original.textContent=money(course.originalPrice);prices.append(original);}
  const current=document.createElement('strong');current.textContent=course.free?'Miễn phí':course.currentPrice==null?'Giá chưa cập nhật':money(course.currentPrice);prices.append(current);
  if(course.discountActive){const badge=document.createElement('span');badge.className='chat-discount';badge.textContent='Giảm '+new Intl.NumberFormat('vi-VN',{maximumFractionDigits:1}).format(course.discountPercent)+'%';prices.append(badge);}
  a.append(prices);
  if(course.discountEndsAt){const deadline=document.createElement('small');const match=/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(course.discountEndsAt);deadline.textContent=match?'Ưu đãi đến '+match[4]+':'+match[5]+' ngày '+match[3]+'/'+match[2]+'/'+match[1]+' (giờ Việt Nam)':'Xem thời hạn ưu đãi trong khóa học';a.append(deadline);}
  const cta=document.createElement('span');cta.className='chat-course-cta';cta.textContent=course.enrolled && course.accessible?'Tiếp tục học →':'Xem khóa học →';a.append(cta);
 }
 links.append(a);}el.append(links);}messages.append(el);messages.scrollTop=messages.scrollHeight;return el;}
const welcome=()=>append('assistant','Chào bạn! Bạn muốn học để giao tiếp, phục vụ công việc hay luyện IELTS? Tôi sẽ dùng kết quả học tập của bạn để gợi ý bước tiếp theo. Nếu bạn chưa có kết quả, mình bắt đầu bằng bài đầu vào nhé.');
async function api(method,body){const headers={'Content-Type':'application/json'};if(csrf)headers[csrfHeader]=csrf;const response=await fetch('/api/learning-chat',{method,headers,body:body?JSON.stringify(body):undefined});if(!response.ok){if(response.status===401||response.redirected)throw Error('Phiên đăng nhập đã hết. Hãy đăng nhập lại rồi thử gửi.');if(response.status===429)throw Error('Trợ lý đang trả lời. Hãy chờ một chút rồi thử lại.');throw Error('Chưa gửi được tin nhắn. Nội dung của bạn vẫn được giữ để thử lại.');}if(response.redirected)throw Error('Hãy đăng nhập lại để tiếp tục.');return response.json();}
async function submit(){const text=input.value.trim();if(busy||!text)return;controls(true);status.textContent='Đang xem hồ sơ và tìm bài phù hợp…';const bubble=append('user',text);
 try{const reply=await api('POST',{message:text});input.value='';append('assistant',reply.message,reply.actions);status.textContent=reply.fallback?'Đang dùng gợi ý từ hệ thống vì trợ lý chưa kết nối được.':'';}
 catch(error){bubble.remove();status.textContent=error.message;}finally{controls(false);input.focus();}}
form.addEventListener('submit',event=>{event.preventDefault();submit();});input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();submit();}});
document.querySelectorAll('#chat-suggestions button').forEach(button=>button.addEventListener('click',()=>{input.value=button.textContent;submit();}));
clear.addEventListener('click',async()=>{if(busy)return;controls(true);try{await api('DELETE');messages.replaceChildren();welcome();status.textContent='Đã bắt đầu cuộc trò chuyện mới.';}catch(error){status.textContent=error.message;}finally{controls(false);}});
api('GET').then(history=>{if(history.length)history.forEach(item=>append(item.role,item.message,item.actions||[]));else welcome();}).catch(error=>{welcome();status.textContent=error.message;}).finally(()=>controls(false));
})();
