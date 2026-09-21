document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const chatMessages = document.getElementById('chat-messages');
  const chatInput = document.getElementById('chat-input');
  const btnSend = document.getElementById('btn-send');
  const streamToggle = document.getElementById('stream-toggle');
  const welcomeHero = document.getElementById('welcome-hero');
  const conversationList = document.getElementById('conversation-list');
  const btnNewChat = document.getElementById('btn-new-chat');
  const currentSessionLabel = document.getElementById('current-session-id');

  // Knowledge Drawer Elements
  const btnToggleKnowledge = document.getElementById('btn-toggle-knowledge');
  const btnCloseDrawer = document.getElementById('btn-close-drawer');
  const knowledgeDrawer = document.getElementById('knowledge-drawer');
  const uploadDropzone = document.getElementById('upload-dropzone');
  const fileInput = document.getElementById('file-input');
  const uploadStatus = document.getElementById('upload-status');
  const docList = document.getElementById('doc-list');
  const searchTestInput = document.getElementById('search-test-input');
  const btnTestSearch = document.getElementById('btn-test-search');
  const searchTestResults = document.getElementById('search-test-results');

  let currentSessionId = 'session-' + Date.now().toString(36);
  currentSessionLabel.textContent = currentSessionId;

  // Auto resize input
  chatInput.addEventListener('input', () => {
    chatInput.style.height = 'auto';
    chatInput.style.height = Math.min(chatInput.scrollHeight, 140) + 'px';
  });

  // Handle enter key
  chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  btnSend.addEventListener('click', sendMessage);

  // Quick Prompt cards
  document.querySelectorAll('.prompt-card').forEach(card => {
    card.addEventListener('click', () => {
      const prompt = card.getAttribute('data-prompt');
      if (prompt) {
        chatInput.value = prompt;
        sendMessage();
      }
    });
  });

  // New Chat
  btnNewChat.addEventListener('click', () => {
    currentSessionId = 'session-' + Date.now().toString(36);
    currentSessionLabel.textContent = currentSessionId;
    chatMessages.innerHTML = '';
    chatMessages.appendChild(welcomeHero);
    welcomeHero.style.display = 'block';
    refreshConversations();
  });

  // Toggle Drawer
  btnToggleKnowledge.addEventListener('click', () => {
    knowledgeDrawer.classList.toggle('closed');
    if (!knowledgeDrawer.classList.contains('closed')) {
      loadDocuments();
    }
  });

  btnCloseDrawer.addEventListener('click', () => {
    knowledgeDrawer.classList.add('closed');
  });

  // File Upload Handlers
  uploadDropzone.addEventListener('click', () => fileInput.click());

  uploadDropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadDropzone.classList.add('dragover');
  });

  uploadDropzone.addEventListener('dragleave', () => {
    uploadDropzone.classList.remove('dragover');
  });

  uploadDropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadDropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
      uploadFile(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener('change', () => {
    if (fileInput.files.length > 0) {
      uploadFile(fileInput.files[0]);
    }
  });

  // Send Message Logic
  async function sendMessage() {
    const text = chatInput.value.trim();
    if (!text || btnSend.disabled) return;

    if (welcomeHero) {
      welcomeHero.style.display = 'none';
    }

    appendUserMessage(text);
    chatInput.value = '';
    chatInput.style.height = 'auto';
    btnSend.disabled = true;

    const useStream = streamToggle.checked;
    if (useStream) {
      await handleStreamMessage(text);
    } else {
      await handleSyncMessage(text);
    }

    btnSend.disabled = false;
    refreshConversations();
  }

  function appendUserMessage(text) {
    const row = document.createElement('div');
    row.className = 'message-row user-row';
    row.innerHTML = `
      <div class="message-bubble">${escapeHtml(text)}</div>
      <div class="avatar user-avatar">U</div>
    `;
    chatMessages.appendChild(row);
    scrollToBottom();
  }

  function createAiMessageRow() {
    const row = document.createElement('div');
    row.className = 'message-row ai-row';
    row.innerHTML = `
      <div class="avatar ai-avatar">✨</div>
      <div class="message-bubble">
        <div class="ai-text-content">
          <span class="status-indicator">Thinking...</span>
        </div>
      </div>
    `;
    chatMessages.appendChild(row);
    scrollToBottom();
    return row;
  }

  // SSE Stream Handler
  async function handleStreamMessage(text) {
    const row = createAiMessageRow();
    const textContainer = row.querySelector('.ai-text-content');
    let accumulatedText = '';

    const streamUrl = `/api/chat/stream?message=${encodeURIComponent(text)}&conversationId=${encodeURIComponent(currentSessionId)}`;
    const eventSource = new EventSource(streamUrl);

    return new Promise((resolve) => {
      eventSource.addEventListener('status', (e) => {
        textContainer.innerHTML = `<span style="color: #38bdf8; font-size: 0.85rem;">⚡ ${escapeHtml(e.data)}</span>`;
        scrollToBottom();
      });

      eventSource.addEventListener('chunk', (e) => {
        accumulatedText += e.data;
        textContainer.innerHTML = renderMarkdown(accumulatedText);
        scrollToBottom();
      });

      eventSource.addEventListener('done', () => {
        eventSource.close();
        textContainer.innerHTML = renderMarkdown(accumulatedText || "Done.");
        scrollToBottom();
        resolve();
      });

      eventSource.addEventListener('error', (e) => {
        eventSource.close();
        if (accumulatedText) {
          textContainer.innerHTML = renderMarkdown(accumulatedText);
        } else {
          textContainer.innerHTML = `<span style="color: #f87171;">⚠️ Stream encountered an issue. Please verify GEMINI_API_KEY.</span>`;
        }
        scrollToBottom();
        resolve();
      });
    });
  }

  // Synchronous POST Handler
  async function handleSyncMessage(text) {
    const row = createAiMessageRow();
    const textContainer = row.querySelector('.ai-text-content');

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text, conversationId: currentSessionId })
      });

      const data = await response.json();
      textContainer.innerHTML = renderMarkdown(data.response || "No response received.");
    } catch (err) {
      textContainer.innerHTML = `<span style="color: #f87171;">Error: ${escapeHtml(err.message)}</span>`;
    }
    scrollToBottom();
  }

  // Upload Document
  async function uploadFile(file) {
    uploadStatus.textContent = `Uploading ${file.name}...`;
    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await fetch('/api/knowledge/upload', {
        method: 'POST',
        body: formData
      });
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      uploadStatus.textContent = `✅ Successfully indexed ${data.filename} (${data.chunkCount} chunks)!`;
      loadDocuments();
    } catch (err) {
      uploadStatus.textContent = `❌ Upload failed: ${err.message}`;
    }
  }

  // Load Indexed Documents
  async function loadDocuments() {
    try {
      const res = await fetch('/api/knowledge/documents');
      const docs = await res.json();
      docList.innerHTML = '';

      if (docs.length === 0) {
        docList.innerHTML = '<div style="color: var(--text-muted); font-size: 0.8rem;">No documents indexed yet.</div>';
        return;
      }

      docs.forEach(doc => {
        const item = document.createElement('div');
        item.className = 'doc-card';
        item.innerHTML = `
          <div class="doc-info">
            <span class="doc-name" title="${escapeHtml(doc.filename)}">📄 ${escapeHtml(doc.filename)}</span>
            <span class="doc-meta">${doc.chunkCount} chunks • ${(doc.fileSizeBytes / 1024).toFixed(1)} KB</span>
          </div>
        `;
        docList.appendChild(item);
      });
    } catch (err) {
      console.warn('Failed to load documents:', err);
    }
  }

  // Test Vector Search
  btnTestSearch.addEventListener('click', async () => {
    const q = searchTestInput.value.trim();
    if (!q) return;

    searchTestResults.innerHTML = '<div style="color: var(--text-muted);">Searching embeddings...</div>';
    try {
      const res = await fetch(`/api/knowledge/search?q=${encodeURIComponent(q)}&topK=3`);
      const results = await res.json();
      searchTestResults.innerHTML = '';

      if (results.length === 0) {
        searchTestResults.innerHTML = '<div style="color: var(--text-muted);">No matches found.</div>';
        return;
      }

      results.forEach((r, idx) => {
        const item = document.createElement('div');
        item.className = 'search-result-snippet';
        const src = r.metadata && r.metadata.source ? r.metadata.source : 'Document';
        item.innerHTML = `<strong>#${idx + 1} (${src}):</strong> ${escapeHtml(r.content.substring(0, 140))}...`;
        searchTestResults.appendChild(item);
      });
    } catch (err) {
      searchTestResults.innerHTML = `<div style="color: #f87171;">Search error: ${err.message}</div>`;
    }
  });

  // Conversations List
  async function refreshConversations() {
    try {
      const res = await fetch('/api/conversations');
      const sessions = await res.json();
      conversationList.innerHTML = '';

      sessions.forEach(sessId => {
        const item = document.createElement('div');
        item.className = 'conv-item' + (sessId === currentSessionId ? ' active' : '');
        item.innerHTML = `
          <span class="conv-name" title="${escapeHtml(sessId)}">💬 ${escapeHtml(sessId)}</span>
        `;
        item.addEventListener('click', () => {
          currentSessionId = sessId;
          currentSessionLabel.textContent = currentSessionId;
          document.querySelectorAll('.conv-item').forEach(i => i.classList.remove('active'));
          item.classList.add('active');
        });
        conversationList.appendChild(item);
      });
    } catch (err) {
      console.warn('Failed to fetch conversations:', err);
    }
  }

  // Helpers
  function renderMarkdown(content) {
    if (typeof marked !== 'undefined' && marked.parse) {
      return marked.parse(content);
    }
    return escapeHtml(content);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
  }

  function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  // Initial loads
  refreshConversations();
  loadDocuments();
});
