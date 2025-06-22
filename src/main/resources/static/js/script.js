/**
 * 曲中人 - 音乐播放器脚本
 * 支持播放列表、播放模式控制、歌曲管理等功能
 */

// 全局变量
let currentPlaylist = []; // 当前播放列表
let currentSongIndex = 0; // 当前播放歌曲索引
let recommendedSongs = []; // 推荐歌曲列表
let isPlaying = false;
let isMuted = false;
let playMode = 'loop'; // 播放模式: loop(列表循环), single(单曲循环), random(随机), order(顺序)
let selectedGenres = ['pop']; // 默认选中流行音乐
let selectedRegions = ['china']; // 默认选中中国

/**
 * 初始化标签选择功能
 */
function initializeTags() {
    // 初始化流派标签
    const genreTags = document.querySelectorAll('#genreTags .tag');
    genreTags.forEach(tag => {
        tag.addEventListener('click', function() {
            const genre = this.dataset.genre;
            toggleTag(this, genre, selectedGenres);
        });
    });
    
    // 初始化地区标签
    const regionTags = document.querySelectorAll('#regionTags .tag');
    regionTags.forEach(tag => {
        tag.addEventListener('click', function() {
            const region = this.dataset.region;
            toggleTag(this, region, selectedRegions);
        });
    });
}

/**
 * 切换标签选中状态
 */
function toggleTag(tagElement, value, selectedArray) {
    if (tagElement.classList.contains('active')) {
        // 如果是最后一个选中的标签，不允许取消
        if (selectedArray.length <= 1) {
            return;
        }
        tagElement.classList.remove('active');
        const index = selectedArray.indexOf(value);
        if (index > -1) {
            selectedArray.splice(index, 1);
        }
    } else {
        tagElement.classList.add('active');
        if (!selectedArray.includes(value)) {
            selectedArray.push(value);
        }
    }
}

// 页面加载完成后初始化标签功能
document.addEventListener('DOMContentLoaded', function() {
    initializeTags();
});

/**
 * 显示错误消息
 */
function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    errorDiv.innerHTML = `<div class="error">${message}</div>`;
    setTimeout(() => {
        errorDiv.innerHTML = '';
    }, 5000);
}

/**
 * 显示加载状态
 */
function showLoading() {
    const answerSection = document.getElementById('answerSection');
    const answerContent = document.getElementById('answerContent');
    
    answerContent.innerHTML = `<div class="loading">${I18N.loading}</div>`;
    answerSection.classList.add('show');
    
    // 滚动到答案区域
    answerSection.scrollIntoView({ behavior: 'smooth' });
}

/**
 * 显示答案
 */
// 存储解析出的歌曲信息
let parsedSongs = [];

/**
 * 格式化歌词显示 - 解析歌词信息
 */
function formatLyrics(answer) {
    if (!answer || answer.trim() === '') {
        return answer;
    }
    
    // 分割每行歌词并解析歌曲信息
    const lines = answer.split('\n');
    const songLyrics = [];
    
    lines.forEach((line, index) => {
        line = line.trim();
        if (line === '') return;
        
        // 检查是否是歌词格式：歌词内容--歌手《歌名》
        if (line.includes('--') && line.includes('《') && line.includes('》')) {
            const parts = line.split('--');
            if (parts.length === 2) {
                const lyricText = parts[0].trim();
                const songInfo = parts[1].trim();
                
                // 提取歌手和歌名
                const artistMatch = songInfo.match(/^(.+?)《(.+?)》$/);
                if (artistMatch) {
                    const artist = artistMatch[1].trim();
                    const songName = artistMatch[2].trim();
                    
                    songLyrics.push({
                        lyricText,
                        songInfo,
                        artist,
                        songName
                    });
                }
            }
        }
    });
    
    // 存储解析出的歌曲信息供后续使用
    parsedSongs = songLyrics;
    
    // 返回空字符串，歌词将通过displayCurrentSongLyrics函数显示
    return '';
}

/**
 * 显示当前播放歌曲的歌词
 */
function displayCurrentSongLyrics() {
    const answerContent = document.getElementById('answerContent');
    
    // 检查是否有当前播放的歌曲
    if (currentPlaylist.length === 0 || currentSongIndex < 0 || currentSongIndex >= currentPlaylist.length) {
        answerContent.innerHTML = '<div class="no-song-message">暂无播放歌曲</div>';
        return;
    }
    
    const currentSong = currentPlaylist[currentSongIndex];
    
    // 从解析的歌曲中找到当前播放歌曲对应的歌词解答
    let currentSongLyricAnswer = '';
    if (parsedSongs && parsedSongs.length > 0) {
        const matchedSong = parsedSongs.find(song => 
            song.artist === currentSong.artist && song.songName === currentSong.song
        );
        if (matchedSong) {
            currentSongLyricAnswer = matchedSong.lyricText;
        }
    }
    
    // 如果没有找到对应的歌词解答，显示提示信息
    if (!currentSongLyricAnswer) {
        answerContent.innerHTML = '<div class="no-lyrics-message">当前歌曲暂无AI歌词解答</div>';
        return;
    }
    
    // 显示AI歌词解答片段，结尾带上歌曲和歌手信息
    answerContent.innerHTML = `
        <div class="current-lyrics-text">
            ${escapeHtml(currentSongLyricAnswer)} &nbsp;&nbsp;&nbsp;&nbsp;—— 《${escapeHtml(currentSong.song)}》 ${escapeHtml(currentSong.artist)}
        </div>
    `;
}

/**
 * HTML转义函数
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 自动添加解析出的歌曲到播放列表
 */
async function autoAddParsedSongsToPlaylist() {
    showToast(`正在搜索并添加 ${parsedSongs.length} 首歌曲到播放列表...`);
    
    for (const song of parsedSongs) {
        try {
            const response = await fetch('/api/music/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    artist: song.artist,
                    songName: song.songName
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                const musicInfo = result.data;
                if (musicInfo && musicInfo.songId) {
                    // 检查歌曲是否已在播放列表中
                    const existingIndex = currentPlaylist.findIndex(s => s.songId === musicInfo.songId);
                    if (existingIndex === -1) {
                        // 添加歌词信息到音乐信息中
                        musicInfo.lyricText = song.lyricText;
                        currentPlaylist.push(musicInfo);
                    }
                }
            }
        } catch (error) {
            console.error(`搜索歌曲失败: ${song.artist} - ${song.songName}`, error);
        }
    }
    
    updatePlaylistDisplay();
    showToast(`已添加 ${currentPlaylist.length} 首歌曲到播放列表`);
    
    // 如果播放列表不为空且当前没有播放歌曲，自动播放第一首
    if (currentPlaylist.length > 0 && currentSongIndex === -1) {
        currentSongIndex = 0;
        loadAndPlaySong(currentPlaylist[0]);
    }
    
    return Promise.resolve();
}

/**
 * 播放歌词中的歌曲
 */
async function playLyricSong(artist, songName) {
    try {
        showToast('正在搜索歌曲...');
        
        // 搜索歌曲
        const response = await fetch('/api/music/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                artist: artist,
                songName: songName
            })
        });
        
        if (response.ok) {
             const result = await response.json();
             const musicInfo = result.data;
             if (musicInfo && musicInfo.songId) {
                 // 清空当前播放列表并添加新歌曲
                 currentPlaylist = [musicInfo];
                 currentIndex = 0;
                 
                 // 立即播放
                loadAndPlaySong(musicInfo);
                 updatePlaylistDisplay();
                 showToast(`正在播放：${artist} - ${songName}`);
             } else {
                 showToast('未找到该歌曲，请尝试其他歌曲');
             }
         } else {
            showToast('搜索歌曲失败，请稍后重试');
        }
    } catch (error) {
        console.error('播放歌曲失败:', error);
        showToast('播放失败，请检查网络连接');
    }
}

/**
 * 添加歌词中的歌曲到播放列表
 */
async function addLyricToPlaylist(artist, songName) {
    try {
        showToast('正在搜索歌曲...');
        
        // 搜索歌曲
        const response = await fetch('/api/music/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                artist: artist,
                songName: songName
            })
        });
        
        if (response.ok) {
             const result = await response.json();
             const musicInfo = result.data;
             if (musicInfo && musicInfo.songId) {
                 // 检查歌曲是否已在播放列表中
                 const existingIndex = currentPlaylist.findIndex(song => song.songId === musicInfo.songId);
                 if (existingIndex !== -1) {
                     showToast('该歌曲已在播放列表中');
                     return;
                 }
                 
                 // 添加到当前播放歌曲的下一首位置
                 const insertIndex = currentIndex + 1;
                 currentPlaylist.splice(insertIndex, 0, musicInfo);
                 
                 updatePlaylistDisplay();
                 showToast(`已添加到播放列表下一首：${artist} - ${songName}`);
             } else {
                 showToast('未找到该歌曲，请尝试其他歌曲');
             }
         } else {
            showToast('搜索歌曲失败，请稍后重试');
        }
    } catch (error) {
        console.error('添加歌曲失败:', error);
        showToast('添加失败，请检查网络连接');
    }
}

/**
 * 显示提示消息
 */
function showToast(message) {
    // 移除现有的提示消息
    const existingToast = document.querySelector('.toast-message');
    if (existingToast) {
        existingToast.remove();
    }
    
    // 创建新的提示消息
    const toast = document.createElement('div');
    toast.className = 'toast-message';
    toast.textContent = message;
    
    document.body.appendChild(toast);
    
    // 显示动画
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    // 3秒后自动隐藏
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

// 存储AI生成的歌词解答内容
let aiLyricsAnswer = '';

function showAnswer(answer, musicList = null) {
    // 保存AI生成的歌词解答内容
    aiLyricsAnswer = answer;
    
    // 解析歌词信息
    formatLyrics(answer);

    // 自动搜索并添加解析出的歌曲到播放列表
    if (parsedSongs && parsedSongs.length > 0) {
        autoAddParsedSongsToPlaylist().then(() => {
            // 添加完成后显示AI歌词解答
            displayCurrentSongLyrics();
        });
        showMusicPlayer();
    } else {
        // 如果没有解析出歌曲，显示AI歌词解答
        displayCurrentSongLyrics();
    }

    // 如果有音乐信息，显示播放器和歌曲列表
    if (musicList && musicList.length > 0) {
        recommendedSongs = musicList;
        showMusicPlayer();
        showRecommendedSongs(musicList);
        
        // 如果播放列表为空，自动添加第一首歌曲
        if (currentPlaylist.length === 0) {
            addToPlaylist(musicList[0]);
        }
    } else if (!parsedSongs || parsedSongs.length === 0) {
        hideMusicPlayer();
    }
}

/**
 * 显示音乐播放器
 */
function showMusicPlayer() {
    const musicPlayer = document.getElementById('musicPlayer');
    musicPlayer.style.display = 'block';
    
    // 更新播放模式按钮显示
    updatePlayModeButton();
}

/**
 * 隐藏音乐播放器
 */
function hideMusicPlayer() {
    const musicPlayer = document.getElementById('musicPlayer');
    const audioPlayer = document.getElementById('audioPlayer');

    musicPlayer.style.display = 'none';
    audioPlayer.pause();
    audioPlayer.src = '';
    resetPlayer();
    
    // 清空相关数据
    currentPlaylist = [];
    recommendedSongs = [];
    currentSongIndex = 0;
}

/**
 * 显示推荐歌曲列表
 */
function showRecommendedSongs(musicList) {
    const recommendedSongs = document.getElementById('recommendedSongs');
    const songsList = document.getElementById('songsList');
    
    let songsHtml = '';
    musicList.forEach((music, index) => {
        songsHtml += `
            <div class="song-item" data-index="${index}">
                <div class="song-info">
                    <div class="song-title">${music.song}</div>
                    <div class="song-artist">${music.artist}</div>
                </div>
                <div class="song-actions">
                    <button class="action-btn" onclick="playNow(${index})" title="立即播放">▶</button>
                    <button class="action-btn" onclick="addToPlaylistFromRecommended(${index})" title="添加到播放列表">+</button>
                </div>
            </div>
        `;
    });
    
    songsList.innerHTML = songsHtml;
    recommendedSongs.style.display = 'block';
}

/**
 * 验证问题输入
 */
function validateQuestion(question) {
    if (!question || question.trim() === '') {
        showError(I18N.error.empty);
        return false;
    }
    
    if (question.length > MAX_QUESTION_LENGTH) {
        showError(I18N.error.length);
        return false;
    }
    
    return true;
}

/**
 * 主要的提问函数
 */
async function askQuestion() {
    const questionInput = document.getElementById('questionInput');
    const submitBtn = document.getElementById('submitBtn');
    const question = questionInput.value.trim();
    
    // 验证输入
    if (!validateQuestion(question)) {
        return;
    }
    
    // 获取并验证歌曲数量
    const musicCountInput = document.getElementById('musicCountInput');
    let musicCount = parseInt(musicCountInput.value) || 5;
    
    // 限制歌曲数量在1-10之间
    if (musicCount < 1) {
        musicCount = 1;
        musicCountInput.value = 1;
    } else if (musicCount > 10) {
        musicCount = 10;
        musicCountInput.value = 10;
    }
    
    // 禁用按钮，防止重复提交
    submitBtn.disabled = true;
    submitBtn.textContent = I18N.thinking;
    
    // 清除之前的错误消息
    document.getElementById('errorMessage').innerHTML = '';
    
    try {
        // 显示加载状态
        showLoading();

        // 发送请求到后端
        const response = await fetch('/api/ask', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept-Language': navigator.language || 'zh-CN'
            },
            body: JSON.stringify({
                question: question,
                musicCount: musicCount,
                genres: selectedGenres,
                regions: selectedRegions
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // 显示答案
            showAnswer(result.data.answer, result.data.musicList);

            // 如果没有音乐信息，显示提示（但不影响主要功能）
            if (!result.data.musicList || result.data.musicList.length === 0) {
                console.log(I18N.music.notFound);
            }
        } else {
            // 显示错误
            showError(result.error || I18N.error.server);
            document.getElementById('answerSection').classList.remove('show');
        }
        
    } catch (error) {
        console.error('请求失败:', error);
        showError(I18N.error.network);
        document.getElementById('answerSection').classList.remove('show');
    } finally {
        // 恢复按钮状态
        submitBtn.disabled = false;
        submitBtn.textContent = I18N.findAnswer;
    }
}

// ==================== 播放器控制函数 ====================

/**
 * 播放/暂停切换
 */
function togglePlay() {
    const audioPlayer = document.getElementById('audioPlayer');
    const playBtn = document.getElementById('playBtn');
    const playIcon = playBtn.querySelector('.play-icon');
    const pauseIcon = playBtn.querySelector('.pause-icon');

    if (isPlaying) {
        audioPlayer.pause();
        playIcon.style.display = 'inline';
        pauseIcon.style.display = 'none';
        isPlaying = false;
    } else {
        if (currentPlaylist.length > 0) {
            audioPlayer.play().catch(error => {
                console.error('播放失败:', error);
                showError(I18N.error.play);
            });
            playIcon.style.display = 'none';
            pauseIcon.style.display = 'inline';
            isPlaying = true;
        }
    }
}

/**
 * 播放上一首
 */
function playPrevious() {
    if (currentPlaylist.length === 0) return;
    
    if (playMode === 'random') {
        currentSongIndex = Math.floor(Math.random() * currentPlaylist.length);
    } else {
        currentSongIndex = currentSongIndex > 0 ? currentSongIndex - 1 : currentPlaylist.length - 1;
    }
    
    loadAndPlaySong(currentPlaylist[currentSongIndex]);
}

/**
 * 播放下一首
 */
function playNext() {
    if (currentPlaylist.length === 0) return;
    
    if (playMode === 'random') {
        currentSongIndex = Math.floor(Math.random() * currentPlaylist.length);
    } else {
        currentSongIndex = currentSongIndex < currentPlaylist.length - 1 ? currentSongIndex + 1 : 0;
    }
    
    loadAndPlaySong(currentPlaylist[currentSongIndex]);
}

/**
 * 切换播放模式
 */
function togglePlayMode() {
    const modes = ['loop', 'single', 'random', 'order'];
    const currentIndex = modes.indexOf(playMode);
    playMode = modes[(currentIndex + 1) % modes.length];
    
    updatePlayModeButton();
}

/**
 * 更新播放模式按钮显示
 */
function updatePlayModeButton() {
    const playModeBtn = document.getElementById('playModeBtn');
    const modeIcons = {
        'loop': '🔁',
        'single': '🔂', 
        'random': '🔀',
        'order': '▶️'
    };
    
    playModeBtn.textContent = modeIcons[playMode];
    playModeBtn.title = getModeTitle(playMode);
}

/**
 * 获取播放模式标题
 */
function getModeTitle(mode) {
    const titles = {
        'loop': '列表循环',
        'single': '单曲循环',
        'random': '随机播放',
        'order': '顺序播放'
    };
    return titles[mode] || '列表循环';
}

/**
 * 静音切换
 */
function toggleMute() {
    const audioPlayer = document.getElementById('audioPlayer');
    const volumeBtn = document.querySelector('.volume-btn');

    if (isMuted) {
        audioPlayer.muted = false;
        volumeBtn.textContent = '🔊';
        isMuted = false;
    } else {
        audioPlayer.muted = true;
        volumeBtn.textContent = '🔇';
        isMuted = true;
    }
}

/**
 * 切换播放列表显示
 */
function togglePlaylist() {
    const playlistContainer = document.getElementById('playlistContainer');
    const isVisible = playlistContainer.style.display !== 'none';
    
    playlistContainer.style.display = isVisible ? 'none' : 'block';
    
    if (!isVisible) {
        updatePlaylistDisplay();
    }
}

// ==================== 播放列表管理 ====================

/**
 * 添加歌曲到播放列表
 */
function addToPlaylist(music) {
    // 检查是否已存在
    const exists = currentPlaylist.some(item => item.songId === music.songId);
    if (!exists) {
        currentPlaylist.push(music);
        updatePlaylistDisplay();
        
        // 如果是第一首歌曲，自动加载
        if (currentPlaylist.length === 1) {
            currentSongIndex = 0;
            loadAndPlaySong(music);
        }
    }
}

/**
 * 从推荐列表添加到播放列表
 */
function addToPlaylistFromRecommended(index) {
    if (recommendedSongs[index]) {
        addToPlaylist(recommendedSongs[index]);
    }
}

/**
 * 立即播放推荐歌曲
 */
function playNow(index) {
    if (recommendedSongs[index]) {
        addToPlaylist(recommendedSongs[index]);
        // 找到在播放列表中的位置
        const playlistIndex = currentPlaylist.findIndex(item => item.songId === recommendedSongs[index].songId);
        if (playlistIndex !== -1) {
            currentSongIndex = playlistIndex;
            loadAndPlaySong(currentPlaylist[currentSongIndex]);
        }
    }
}

/**
 * 全部添加到播放列表
 */
function addAllToPlaylist() {
    const addedCount = recommendedSongs.length;
    recommendedSongs.forEach(music => {
        addToPlaylist(music);
    });
    
    if (addedCount > 0) {
        showToast(`已成功添加 ${addedCount} 首歌曲到播放列表`);
    }
}

/**
 * 清空播放列表
 */
function clearPlaylist() {
    currentPlaylist = [];
    currentSongIndex = 0;
    updatePlaylistDisplay();
    
    // 停止播放
    const audioPlayer = document.getElementById('audioPlayer');
    audioPlayer.pause();
    audioPlayer.src = '';
    resetPlayer();
    
    // 清空歌词解释
    const answerContent = document.getElementById('answerContent');
    answerContent.innerHTML = '<div class="no-song-message">暂无播放歌曲</div>';
}

/**
 * 从播放列表移除歌曲
 */
function removeFromPlaylist(index) {
    if (index < currentPlaylist.length) {
        currentPlaylist.splice(index, 1);
        
        // 调整当前播放索引
        if (index < currentSongIndex) {
            currentSongIndex--;
        } else if (index === currentSongIndex) {
            // 如果删除的是当前播放的歌曲
            if (currentPlaylist.length === 0) {
                // 播放列表为空
                const audioPlayer = document.getElementById('audioPlayer');
                audioPlayer.pause();
                audioPlayer.src = '';
                resetPlayer();
                currentSongIndex = 0;
            } else {
                // 播放下一首（或第一首）
                if (currentSongIndex >= currentPlaylist.length) {
                    currentSongIndex = 0;
                }
                loadAndPlaySong(currentPlaylist[currentSongIndex]);
            }
        }
        
        updatePlaylistDisplay();
    }
}

/**
 * 播放播放列表中的指定歌曲
 */
function playFromPlaylist(index) {
    if (index < currentPlaylist.length) {
        currentSongIndex = index;
        loadAndPlaySong(currentPlaylist[currentSongIndex]);
    }
}

/**
 * 更新播放列表显示
 */
function updatePlaylistDisplay() {
    const playlist = document.getElementById('playlist');
    
    if (currentPlaylist.length === 0) {
        playlist.innerHTML = '<div class="playlist-empty">播放列表为空</div>';
        return;
    }
    
    let playlistHtml = '';
    currentPlaylist.forEach((music, index) => {
        const isCurrentSong = index === currentSongIndex;
        const lyricText = music.lyricText || '';
        
        playlistHtml += `
            <div class="playlist-item ${isCurrentSong ? 'current' : ''}" data-index="${index}">
                <div class="song-info" onclick="playFromPlaylist(${index})">
                    <div class="song-title">${music.song} ${isCurrentSong ? '♪' : ''}</div>
                    <div class="song-artist">${music.artist}</div>
                    ${lyricText ? `<div class="song-lyric-scroll">
                        <div class="lyric-scroll-content">${escapeHtml(lyricText)}</div>
                    </div>` : ''}
                </div>
                <div class="song-actions">
                    <button class="action-btn" onclick="removeFromPlaylist(${index})" title="移除">×</button>
                </div>
            </div>
        `;
    });
    
    playlist.innerHTML = playlistHtml;
}

/**
 * 加载并播放歌曲
 */
function loadAndPlaySong(music) {
    const audioPlayer = document.getElementById('audioPlayer');
    const musicTitle = document.getElementById('musicTitle');
    const musicArtist = document.getElementById('musicArtist');
    
    // 查找并设置当前歌曲索引
    const songIndex = currentPlaylist.findIndex(song => song.songId === music.songId);
    if (songIndex !== -1) {
        currentSongIndex = songIndex;
    }
    
    // 更新显示信息
    musicTitle.textContent = music.song;
    musicArtist.textContent = music.artist;
    
    // 加载音频
    audioPlayer.src = music.playUrl;
    
    // 重置播放器状态
    resetPlayer();
    
    // 自动播放
    audioPlayer.play().then(() => {
        const playBtn = document.getElementById('playBtn');
        const playIcon = playBtn.querySelector('.play-icon');
        const pauseIcon = playBtn.querySelector('.pause-icon');
        
        playIcon.style.display = 'none';
        pauseIcon.style.display = 'inline';
        isPlaying = true;
    }).catch(error => {
        console.error('播放失败:', error);
        showError(I18N.error.play);
    });
    
    // 更新播放列表显示
    updatePlaylistDisplay();
    
    // 更新歌词显示
    displayCurrentSongLyrics();
}

/**
 * 重置播放器状态
 */
function resetPlayer() {
    const playBtn = document.getElementById('playBtn');
    const playIcon = playBtn.querySelector('.play-icon');
    const pauseIcon = playBtn.querySelector('.pause-icon');
    const progressFill = document.getElementById('progressFill');
    const currentTime = document.getElementById('currentTime');
    const duration = document.getElementById('duration');

    playIcon.style.display = 'inline';
    pauseIcon.style.display = 'none';
    progressFill.style.width = '0%';
    currentTime.textContent = '0:00';
    duration.textContent = '0:00';
    isPlaying = false;
}

/**
 * 格式化时间
 */
function formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
}

/**
 * 初始化音频播放器事件
 */
function initAudioPlayer() {
    const audioPlayer = document.getElementById('audioPlayer');
    const progressBar = document.getElementById('progressBar');
    const progressFill = document.getElementById('progressFill');
    const currentTime = document.getElementById('currentTime');
    const duration = document.getElementById('duration');

    // 音频加载完成
    audioPlayer.addEventListener('loadedmetadata', function() {
        duration.textContent = formatTime(audioPlayer.duration);
    });

    // 播放时间更新
    audioPlayer.addEventListener('timeupdate', function() {
        if (audioPlayer.duration) {
            const progress = (audioPlayer.currentTime / audioPlayer.duration) * 100;
            progressFill.style.width = progress + '%';
            currentTime.textContent = formatTime(audioPlayer.currentTime);
        }
    });

    // 播放结束
    audioPlayer.addEventListener('ended', function() {
        handleSongEnd();
    });

    // 播放错误
    audioPlayer.addEventListener('error', function() {
        showError(I18N.error.load);
        resetPlayer();
    });

    // 进度条点击
    progressBar.addEventListener('click', function(e) {
        if (audioPlayer.duration) {
            const rect = progressBar.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const progress = clickX / rect.width;
            audioPlayer.currentTime = progress * audioPlayer.duration;
        }
    });
}

/**
 * 处理歌曲播放结束
 */
function handleSongEnd() {
    switch (playMode) {
        case 'single':
            // 单曲循环，重新播放当前歌曲
            const audioPlayer = document.getElementById('audioPlayer');
            audioPlayer.currentTime = 0;
            audioPlayer.play();
            break;
        case 'order':
            // 顺序播放，播放下一首，到最后一首停止
            if (currentSongIndex < currentPlaylist.length - 1) {
                playNext();
            } else {
                resetPlayer();
            }
            break;
        case 'random':
        case 'loop':
        default:
            // 列表循环或随机播放，播放下一首
            playNext();
            break;
    }
}

/**
 * 页面加载完成后的初始化
 */
document.addEventListener('DOMContentLoaded', function() {
    const questionInput = document.getElementById('questionInput');
    const submitBtn = document.getElementById('submitBtn');
    
    // 添加回车键提交功能
    questionInput.addEventListener('keydown', function(event) {
        if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
            event.preventDefault();
            askQuestion();
        }
    });
    
    // 添加字符计数提示
    questionInput.addEventListener('input', function() {
        const length = this.value.length;
        if (length > MAX_QUESTION_LENGTH - 50) {
            this.style.borderColor = '#ffa427';
        } else {
            this.style.borderColor = 'rgba(238, 238, 238, 0.2)';
        }
    });

    // 初始化音频播放器
    initAudioPlayer();
});

/**
 * 清空播放器状态
 */
/**
 * 重新提问功能
 */
function askAgain() {
    document.getElementById('questionInput').value = '';
    document.getElementById('answerSection').classList.remove('show');
    document.getElementById('questionInput').focus();
    hideMusicPlayer();
}