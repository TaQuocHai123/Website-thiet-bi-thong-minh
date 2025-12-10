// Chatbot JavaScript Handler
class ChatbotAI {
    constructor() {
        this.modal = document.getElementById('chatbot-modal');
        this.helpBtn = document.getElementById('chatbot-help-btn');
        this.closeBtn = document.getElementById('chatbot-close-btn');
        this.messagesContainer = document.getElementById('chatbot-messages');
        this.messageForm = document.getElementById('chatbot-message-form');
        this.messageInput = document.getElementById('chatbot-input');
        this.recommendForm = document.getElementById('chatbot-recommend-form');
        this.recommendInput = document.getElementById('chatbot-recommend-input');
        this.tabBtns = document.querySelectorAll('.tab-btn');
        this.chatTabs = document.querySelectorAll('.chat-tab');

        this.isLoading = false;
        this.conversationHistory = [];

        this.init();
    }

    init() {
        // Event listeners
        this.helpBtn?.addEventListener('click', () => this.toggleModal());
        this.closeBtn?.addEventListener('click', () => this.closeModal());
        this.messageForm?.addEventListener('submit', (e) => this.handleMessageSubmit(e));
        this.recommendForm?.addEventListener('submit', (e) => this.handleRecommendSubmit(e));

        // Tab switching
        this.tabBtns.forEach(btn => {
            btn.addEventListener('click', (e) => this.switchTab(e.target.dataset.tab));
        });

        // Close modal on background click
        this.modal?.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.closeModal();
            }
        });

        // Prevent form submission on Enter in input
        this.messageInput?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.messageForm.dispatchEvent(new Event('submit'));
            }
        });

        this.recommendInput?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.recommendForm.dispatchEvent(new Event('submit'));
            }
        });

        // Initialize quick option buttons
        this.initQuickOptions();
    }

    initQuickOptions() {
        const optionsContainer = document.getElementById('chatbot-quick-options');

        // If static container exists (in some templates), wire its buttons
        if (optionsContainer) {
            optionsContainer.querySelectorAll('.quick-option').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const type = btn.dataset.type;
                    this._handleQuickOptionClick(btn.textContent, type);
                });
            });
        }

        // Fetch options from backend and render them as a bot message inside the chat
        (async () => {
            try {
                const resp = await fetch('/api/chat/options');
                if (!resp.ok) throw new Error('HTTP ' + resp.status);
                const data = await resp.json();

                // Determine options array shape flexibly
                let options = [];
                if (Array.isArray(data)) {
                    options = data;
                } else if (Array.isArray(data.options)) {
                    options = data.options;
                } else if (Array.isArray(data.labels)) {
                    options = data.labels;
                }

                if (options && options.length) {
                    this.renderQuickOptionsMessage(options);
                }
            } catch (err) {
                console.warn('Could not load chatbot quick options', err);
            }
        })();

        // Delegated click handler for quick-option buttons inside chat messages
        this.messagesContainer?.addEventListener('click', async (e) => {
            const btn = e.target.closest('.quick-option');
            if (!btn) return;
            const type = btn.dataset.type;
            const label = btn.textContent || btn.innerText || type;
            this._handleQuickOptionClick(label, type);
        });
    }

    // Render a bot message that contains the quick option buttons so users always see them in the chat
    renderQuickOptionsMessage(options) {
        const parts = [];
        parts.push('<div class="chatbot-quick-options-message">');
        parts.push('<p style="margin-bottom: 10px; font-weight: 500;">Hãy nhập câu hỏi của bạn dưới đây 👇</p>');
        parts.push('<div class="quick-options-buttons" style="display: flex; flex-direction: column; gap: 8px;">');

        options.forEach(opt => {
            // support options given as string or object {type,label}
            const type = (typeof opt === 'string') ? opt : (opt.type || opt.key || opt.name);
            const label = (typeof opt === 'string') ? opt : (opt.label || opt.text || opt.name || type);
            parts.push(`<button class="quick-option" data-type="${type}" style="padding: 10px 12px; text-align: left; border: 1px solid #e0e0e0; border-radius: 6px; background: #f8f9fa; cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.2s; color: #333;">${label}</button>`);
        });

        parts.push('</div>');
        parts.push('</div>');

        const html = parts.join('');
        this.addMessage(html, 'bot');
    }

    // Internal handler shared by both static buttons and message buttons
    async _handleQuickOptionClick(label, type) {
        // show user selection
        this.addMessage(label, 'user');

        // typing
        this.showTypingIndicator();

        try {
            const resp = await fetch(`/api/chat/info?type=${encodeURIComponent(type)}`);
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const data = await resp.json();
            this.removeTypingIndicator();
            if (data.success) {
                this.addMessage(data.message, 'bot');
            } else {
                this.addMessage(data.message || 'Không lấy được thông tin.', 'bot', true);
            }
        } catch (err) {
            console.error('Quick option error', err);
            this.removeTypingIndicator();
            this.addMessage('Xin lỗi, không thể lấy thông tin. Vui lòng thử lại.', 'bot', true);
        }
    }

    toggleModal() {
        if (this.modal.classList.contains('active')) {
            this.closeModal();
        } else {
            this.openModal();
        }
    }

    openModal() {
        this.modal?.classList.add('active');
        this.messageInput?.focus();
    }

    closeModal() {
        this.modal?.classList.remove('active');
    }

    switchTab(tabName) {
        // Update tab buttons
        this.tabBtns.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        // Update tab content
        this.chatTabs.forEach(tab => {
            tab.classList.toggle('active', tab.id === `${tabName}-tab`);
        });
    }

    async handleMessageSubmit(e) {
        e.preventDefault();
        const message = this.messageInput.value.trim();

        if (!message || this.isLoading) {
            return;
        }

        // Add user message to UI
        this.addMessage(message, 'user');
        this.messageInput.value = '';
        this.messageInput.disabled = true;

        // Show typing indicator
        this.showTypingIndicator();

        try {
            const response = await fetch('/api/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `message=${encodeURIComponent(message)}`
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            // Remove typing indicator
            this.removeTypingIndicator();

            if (data.success) {
                // If backend returned structured recommendation (model-chosen matches), render them first
                if (data.structured && Array.isArray(data.structured.matches) && data.structured.matches.length) {
                    const matches = data.structured.matches;
                    let parts = [];
                    parts.push('<div class="chatbot-products">');
                    parts.push('<p>Gợi ý từ cửa hàng (ưu tiên nội bộ):</p>');
                    matches.forEach(m => {
                        parts.push('<div class="chatbot-product" style="display:flex;align-items:center;margin:8px 0;">');
                        // find product details in data.products by id if available
                        let p = null;
                        if (Array.isArray(data.products)) p = data.products.find(x => String(x.id) === String(m.id));
                        const url = (p && p.url) ? p.url : (m.url || '#');
                        const image = (p && p.image) ? p.image : (m.image || '/images/no-image.png');
                        const name = m.name || (p && p.name) || 'Sản phẩm';
                        const code = m.code || (p && p.code) || '';
                        const price = (m.price || (p && p.price)) ? Number(m.price || (p && p.price)).toLocaleString() : '';
                        parts.push(`<a href="${url}" target="_blank" style="margin-right:8px;"><img src="${image}" style="width:64px;height:64px;object-fit:cover;border:1px solid #eee;padding:2px;"/></a>`);
                        parts.push('<div>');
                        parts.push(`<a href="${url}" target="_blank"><strong>${name}</strong></a><br/>`);
                        parts.push(`Mã: ${code} - Giá: ${price} VND<br/>`);
                        if (m.reason) parts.push(`<em style="color:#666">Lý do: ${m.reason}</em>`);
                        parts.push('</div>');
                        parts.push('</div>');
                    });
                    parts.push('</div>');
                    this.addMessage(parts.join(''), 'bot');
                } else if (Array.isArray(data.products) && data.products.length) {
                    let parts = [];
                    parts.push('<div class="chatbot-products">');
                    data.products.forEach(p => {
                        parts.push('<div class="chatbot-product" style="display:flex;align-items:center;margin:8px 0;">');
                        parts.push(`<a href="${p.url}" target="_blank" style="margin-right:8px;"><img src="${p.image}" style="width:64px;height:64px;object-fit:cover;border:1px solid #eee;padding:2px;"/></a>`);
                        parts.push('<div>');
                        parts.push(`<a href="${p.url}" target="_blank"><strong>${p.name}</strong></a><br/>`);
                        parts.push(`Mã: ${p.code} - Giá: ${Number(p.price).toLocaleString()} VND`);
                        parts.push('</div>');
                        parts.push('</div>');
                    });
                    parts.push('</div>');
                    this.addMessage(parts.join(''), 'bot');
                } else if (data.recommendation) {
                    this.addMessage(data.recommendation, 'bot');
                } else if (data.message) {
                    this.addMessage(data.message, 'bot');
                } else {
                    this.addMessage('Có lỗi xảy ra. Vui lòng thử lại.', 'bot', true);
                }
            } else {
                this.addMessage(data.message || 'Có lỗi xảy ra. Vui lòng thử lại.', 'bot', true);
            }
        } catch (error) {
            console.error('Error:', error);
            this.removeTypingIndicator();
            this.addMessage('Xin lỗi, có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.', 'bot', true);
        } finally {
            this.messageInput.disabled = false;
            this.messageInput.focus();
        }
    }

    async handleRecommendSubmit(e) {
        e.preventDefault();
        const query = this.recommendInput.value.trim();

        if (!query || this.isLoading) {
            return;
        }

        // Add user message to UI
        this.addMessage(`🛍️ ${query}`, 'user');
        this.recommendInput.value = '';
        this.recommendInput.disabled = true;

        // Show typing indicator
        this.showTypingIndicator();

        try {
            const response = await fetch('/api/chat/recommend', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `query=${encodeURIComponent(query)}`
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            // Remove typing indicator
            this.removeTypingIndicator();

            if (data.success) {
                if (data.structured && Array.isArray(data.structured.matches) && data.structured.matches.length) {
                    const matches = data.structured.matches;
                    let parts = [];
                    parts.push('<div class="chatbot-products">');
                    parts.push('<p>Gợi ý từ cửa hàng (ưu tiên nội bộ):</p>');
                    matches.forEach(m => {
                        let p = null;
                        if (Array.isArray(data.products)) p = data.products.find(x => String(x.id) === String(m.id));
                        const url = (p && p.url) ? p.url : (m.url || '#');
                        const image = (p && p.image) ? p.image : (m.image || '/images/no-image.png');
                        const name = m.name || (p && p.name) || 'Sản phẩm';
                        const code = m.code || (p && p.code) || '';
                        const price = (m.price || (p && p.price)) ? Number(m.price || (p && p.price)).toLocaleString() : '';
                        parts.push('<div class="chatbot-product" style="display:flex;align-items:center;margin:8px 0;">');
                        parts.push(`<a href="${url}" target="_blank" style="margin-right:8px;"><img src="${image}" style="width:64px;height:64px;object-fit:cover;border:1px solid #eee;padding:2px;"/></a>`);
                        parts.push('<div>');
                        parts.push(`<a href="${url}" target="_blank"><strong>${name}</strong></a><br/>`);
                        parts.push(`Mã: ${code} - Giá: ${price} VND<br/>`);
                        if (m.reason) parts.push(`<em style="color:#666">Lý do: ${m.reason}</em>`);
                        parts.push('</div>');
                        parts.push('</div>');
                    });
                    parts.push('</div>');
                    this.addMessage(parts.join(''), 'bot');
                } else if (Array.isArray(data.products) && data.products.length) {
                    // Build HTML list
                    let parts = [];
                    parts.push('<div class="chatbot-products">');
                    parts.push('<p>Dưới đây là gợi ý từ cửa hàng:</p>');
                    data.products.forEach(p => {
                        parts.push('<div class="chatbot-product" style="display:flex;align-items:center;margin:8px 0;">');
                        parts.push(`<a href="${p.url}" target="_blank" style="margin-right:8px;"><img src="${p.image}" style="width:64px;height:64px;object-fit:cover;border:1px solid #eee;padding:2px;"/></a>`);
                        parts.push('<div>');
                        parts.push(`<a href="${p.url}" target="_blank"><strong>${p.name}</strong></a><br/>`);
                        parts.push(`Mã: ${p.code} - Giá: ${Number(p.price).toLocaleString()} VND`);
                        parts.push('</div>');
                        parts.push('</div>');
                    });
                    parts.push('</div>');
                    this.addMessage(parts.join(''), 'bot');
                } else {
                    this.addMessage(data.recommendation || data.message || 'Không có gợi ý phù hợp.', 'bot');
                }
            } else {
                this.addMessage(data.message || 'Không thể lấy gợi ý. Vui lòng thử lại.', 'bot', true);
            }
        } catch (error) {
            console.error('Error:', error);
            this.removeTypingIndicator();
            this.addMessage('Xin lỗi, có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.', 'bot', true);
        } finally {
            this.recommendInput.disabled = false;
            this.recommendInput.focus();
        }
    }

    addMessage(content, type = 'bot', isError = false) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('chatbot-message', `${type}-message`);

        const contentDiv = document.createElement('div');
        contentDiv.classList.add('message-content');
        if (isError) {
            contentDiv.classList.add('error-message');
        }

        // Parse content as HTML if it contains HTML tags
        if (content.includes('<') && content.includes('>')) {
            contentDiv.innerHTML = content;
        } else {
            // Escape HTML and preserve line breaks
            const p = document.createElement('p');
            p.textContent = content;
            contentDiv.appendChild(p);
        }

        messageDiv.appendChild(contentDiv);

        // Add timestamp
        const timeSpan = document.createElement('span');
        timeSpan.classList.add('message-time');
        timeSpan.textContent = this.getCurrentTime();
        messageDiv.appendChild(timeSpan);

        this.messagesContainer.appendChild(messageDiv);

        // Scroll to bottom
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;

        // Store in history
        this.conversationHistory.push({
            type: type,
            content: content,
            timestamp: new Date()
        });
    }

    showTypingIndicator() {
        this.isLoading = true;
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('chatbot-message', 'bot-message', 'typing-message');

        const contentDiv = document.createElement('div');
        contentDiv.classList.add('message-content', 'typing-indicator');
        contentDiv.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';

        messageDiv.appendChild(contentDiv);
        this.messagesContainer.appendChild(messageDiv);

        // Scroll to bottom
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    removeTypingIndicator() {
        this.isLoading = false;
        const typingMessage = document.querySelector('.typing-message');
        if (typingMessage) {
            typingMessage.remove();
        }
    }

    getCurrentTime() {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    }
}

// Initialize chatbot when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.chatbotAI = new ChatbotAI();
});
