(() => {
 const chart=document.querySelector('.activity-chart');if(!chart)return;
 const tooltip=document.createElement('div');tooltip.className='activity-tooltip';tooltip.setAttribute('role','tooltip');tooltip.hidden=true;document.body.appendChild(tooltip);
 function show(event){const day=event.target.closest('[data-activity-tooltip]');if(!day||!day.dataset.activityTooltip)return;tooltip.textContent=day.dataset.activityTooltip;tooltip.hidden=false;move(event);}
 function move(event){if(tooltip.hidden)return;const gap=14,width=tooltip.offsetWidth,height=tooltip.offsetHeight;let left=event.clientX+gap,top=event.clientY-height-gap;if(left+width>window.innerWidth-8)left=event.clientX-width-gap;if(top<8)top=event.clientY+gap;tooltip.style.left=`${Math.max(8,left)}px`;tooltip.style.top=`${Math.max(8,top)}px`;}
 function hide(){tooltip.hidden=true;}
 chart.addEventListener('pointerover',show);chart.addEventListener('pointermove',move);chart.addEventListener('pointerout',event=>{if(!event.relatedTarget?.closest?.('[data-activity-tooltip]'))hide();});
 window.addEventListener('scroll',hide,{passive:true});
})();
