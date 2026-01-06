const API_BASE = "http://localhost:8080/api/v1/drive"; // Server address
let currentUser = ""; // Current user logged in
let selectedFile = null; // File picked by user

// Helper to call server
async function apiCall(url, options = {}) {
  try {
    const response = await fetch(url, options);
    const result = await response.json();
    if (!result.success) {
      throw new Error(result.message || 'Request failed');
    }
    return result.data;
  } catch (error) {
    if (error.message) {
      throw error;
    }
    throw new Error('Network error: ' + error.message);
  }
}

// Get HTML element by ID
function get(id) {
  return document.getElementById(id);
}

// Show message on page
function showMessage(id, text) {
  get(id).textContent = text;
}

// When page loads, set up
window.addEventListener("DOMContentLoaded", async () => {
  console.log("SmartDrive loaded");
  const fileInput = get("fileInput");
  const dropZone = get("dropZone");

  // When user picks file
  fileInput.addEventListener("change", () => {
    selectedFile = fileInput.files[0] || null;
    if (selectedFile) {
      showMessage("dashboard-msg", "File selected: " + selectedFile.name);
    }
  });

  // Set up drag and drop if exists
  if (dropZone) {
    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropZone.classList.add('dragover');
    });
    dropZone.addEventListener('dragleave', (e) => {
      if (!e.relatedTarget || !dropZone.contains(e.relatedTarget)) {
        dropZone.classList.remove('dragover');
      }
    });
    dropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
      const files = e.dataTransfer.files;
      if (files && files.length > 0) {
        selectedFile = files[0];
        showMessage("dashboard-msg", "File selected via drag/drop: " + selectedFile.name);
      }
    });
  }
});

// Logout
get("logoutBtn").addEventListener("click", () => {
  currentUser = "";
  get("userDisplay").textContent = "";
  get("dashboard").classList.add("hidden");
  get("auth-section").classList.remove("hidden");
  showMessage("auth-msg", "");
  showMessage("dashboard-msg", "");
  get("drive-content").textContent = "";
  selectedFile = null;
});

// Authentication
get("loginBtn").addEventListener("click", async () => {
  const username = get("username").value.trim();
  const password = get("password").value.trim();
  if (!username || !password) {
    return showMessage("auth-msg", "Please enter username & password.");
  }
  try {
    const data = await apiCall(`${API_BASE}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });
    currentUser = username;
    get("userDisplay").textContent = username;
    get("auth-section").classList.add("hidden");
    get("dashboard").classList.remove("hidden");
    showMessage("auth-msg", "Welcome, " + data.username + "!");
    await loadDrive();
  } catch (err) {
    showMessage("auth-msg", "Login error: " + err.message);
  }
});

// Register
get("registerBtn").addEventListener("click", async () => {
  const username = get("username").value.trim();
  const password = get("password").value.trim();
  const quota = parseInt(get("quota").value || "1000000");
  if (!username || !password) {
    return showMessage("auth-msg", "Please enter username & password.");
  }
  const user = {
    username: username,
    password: password,
    storageQuotaBytes: quota,
  };
  try {
    const data = await apiCall(`${API_BASE}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(user),
    });
    showMessage("auth-msg", "User registered: " + data.username);
  } catch (err) {
    showMessage("auth-msg", "Registration failed: " + err.message);
  }
});

// Folders
get("createFolderBtn").addEventListener("click", async () => {
  const folderName = get("folderName").value.trim() || "root";
  try {
    const folder = await apiCall(`${API_BASE}/${currentUser}/folders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ folderName }),
    });
    showMessage("dashboard-msg", "Folder created: " + folder.name);
    await loadDrive();
  } catch (err) {
    showMessage("dashboard-msg", "Folder creation failed: " + err.message);
  }
});

get("deleteFolderBtn").addEventListener("click", async () => {
  const name = get("deleteFolderName").value.trim();
  if (!name) {
    return showMessage("dashboard-msg", "Enter folder name.");
  }
  try {
    const deleteResp = await apiCall(`${API_BASE}/${currentUser}/folders/${name}`, {
      method: "DELETE",
    });
    showMessage("dashboard-msg", "Folder deleted: " + deleteResp.itemName);
    await loadDrive();
  } catch (err) {
    showMessage("dashboard-msg", "Delete failed: " + err.message);
  }
});

// Files
get("uploadFileBtn").addEventListener("click", async () => {
  if (!selectedFile) {
    return showMessage("dashboard-msg", "Select a file first.");
  }
  const folderName = get("uploadFolderName").value.trim() || "root";
  const file = selectedFile;
  const extension = file.name.includes('.') ? file.name.split(".").pop() : "bin";
  try {
    showMessage("dashboard-msg", "Generating upload URL...");
    const uploadData = await apiCall(`${API_BASE}/${currentUser}/files/upload-url`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        folderName: folderName,
        fileName: file.name,
        fileSize: file.size,
        extension: extension
      })
    });
    if (!uploadData.uploadUrl) {
      throw new Error("Failed to get upload URL");
    }
    showMessage("dashboard-msg", "Uploading to S3...");
    const uploadRes = await fetch(uploadData.uploadUrl, {
      method: "PUT",
      body: file,
    });
    if (!uploadRes.ok) {
      throw new Error("S3 upload failed: " + uploadRes.statusText);
    }
    showMessage("dashboard-msg", "File uploaded successfully: " + uploadData.fileName);
    await loadDrive();
    selectedFile = null;
  } catch (err) {
    showMessage("dashboard-msg", "Upload failed: " + err.message);
    selectedFile = null;
  }
});

// Rename file
get("renameFileBtn").addEventListener("click", async () => {
  const folderName = get("renameFolderName").value.trim();
  const oldName = get("oldFileName").value.trim();
  const newName = get("newFileName").value.trim();
  if (!folderName || !oldName || !newName) {
    return showMessage("dashboard-msg", "All fields required.");
  }
  try {
    const fileData = await apiCall(`${API_BASE}/${currentUser}/files`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        folderName: folderName,
        oldName: oldName,
        newName: newName
      }),
    });
    showMessage("dashboard-msg", "File renamed to: " + fileData.name);
    await loadDrive();
  } catch (err) {
    showMessage("dashboard-msg", "Rename failed: " + err.message);
  }
});

// Delete file
get("deleteFileBtn").addEventListener("click", async () => {
  const folderName = get("deleteFileFolder").value.trim();
  const fileName = get("deleteFileName").value.trim();
  if (!folderName || !fileName) {
    return showMessage("dashboard-msg", "Folder and file required.");
  }
  try {
    const deleteResp = await apiCall(`${API_BASE}/${currentUser}/files`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ folderName, fileName }),
    });
    showMessage("dashboard-msg", "File deleted: " + deleteResp.itemName);
    await loadDrive();
  } catch (err) {
    showMessage("dashboard-msg", "Delete failed: " + err.message);
  }
});

// Drive view
get("refreshBtn").addEventListener("click", async () => loadDrive());

// Download function
async function downloadFile(folderName, fileName) {
  try {
    showMessage("dashboard-msg", "Generating download URL for " + fileName + "...");
    const downloadData = await apiCall(
      `${API_BASE}/${currentUser}/files/download-url?` +
      `folderName=${encodeURIComponent(folderName)}&` +
      `fileName=${encodeURIComponent(fileName)}`
    );
    if (!downloadData.downloadUrl) {
      throw new Error("Failed to get download URL");
    }
    window.open(downloadData.downloadUrl, "_blank");
    showMessage("dashboard-msg", "Download started for " + fileName);
  } catch (err) {
    showMessage("dashboard-msg", "Download failed: " + err.message);
  }
}

// Load drive
async function loadDrive() {
  if (!currentUser) return;
  const content = get("drive-content");
  const summary = get("storage-summary");
  try {
    const userData = await apiCall(`${API_BASE}/${currentUser}/drive`);
    summary.textContent =
      "Used: " + userData.usedStorageBytes +
      " / Quota: " + userData.storageQuotaBytes + " bytes";
    content.innerHTML = "";
    if (userData.folders && userData.folders.length > 0) {
      userData.folders.forEach(folder => {
        const div = document.createElement("div");
        div.className = "folder";
        const title = document.createElement("h4");
        title.textContent = " " + folder.name;
        div.appendChild(title);
        if (folder.files && folder.files.length > 0) {
          const ul = document.createElement("ul");
          folder.files.forEach(f => {
            const li = document.createElement("li");
            const fileInfo = document.createElement("span");
            fileInfo.textContent =
              f.name + " (" + f.fileExtension + ", " + f.actualSize + " bytes) ";
            li.appendChild(fileInfo);
            const downloadBtn = document.createElement("button");
            downloadBtn.textContent = "Download";
            downloadBtn.className = "download-btn";
            downloadBtn.onclick = () => downloadFile(folder.name, f.name);
            li.appendChild(downloadBtn);
            ul.appendChild(li);
          });
          div.appendChild(ul);
        } else {
          const p = document.createElement("p");
          p.textContent = "No files in this folder.";
          div.appendChild(p);
        }
        content.appendChild(div);
      });
    } else {
      content.textContent = "No folders found. Create your first folder!";
    }
  } catch (err) {
    content.textContent = "Error loading drive: " + err.message;
    summary.textContent = "";
  }
}