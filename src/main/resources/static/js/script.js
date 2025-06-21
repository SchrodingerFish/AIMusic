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
function showAnswer(answer, musicInfo = null) {
    const answerContent = document.getElementById('answerContent');
    answerContent.innerHTML = answer;

    // 如果有音乐信息，显示播放器
    if (musicInfo && musicInfo.playUrl) {
        showMusicPlayer(musicInfo);
    } else {
        hideMusicPlayer();
    }
}

/**
 * 显示音乐播放器
 */
function showMusicPlayer(musicInfo) {
    const musicPlayer = document.getElementById('musicPlayer');
    const musicTitle = document.getElementById('musicTitle');
    const musicArtist = document.getElementById('musicArtist');
    const audioPlayer = document.getElementById('audioPlayer');

    musicTitle.textContent = musicInfo.song;
    musicArtist.textContent = musicInfo.artist;
    audioPlayer.src = musicInfo.playUrl;

    musicPlayer.style.display = 'block';

    // 重置播放器状态
    resetPlayer();
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
            },
            body: JSON.stringify({
                question: question
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // 显示答案
            showAnswer(result.data.answer, result.data.music);

            // 如果没有音乐信息，显示提示（但不影响主要功能）
            if (!result.data.music || !result.data.music.playUrl) {
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
 * 重新提问功能
 */
function askAgain() {
    document.getElementById('questionInput').value = '';
    document.getElementById('answerSection').classList.remove('show');
    document.getElementById('questionInput').focus();
    hideMusicPlayer();
}

// 音乐播放器控制函数
let isPlaying = false;
let isMuted = false;

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
        audioPlayer.play().catch(error => {
            console.error('播放失败:', error);
            showError(I18N.error.play);
        });
        playIcon.style.display = 'none';
        pauseIcon.style.display = 'inline';
        isPlaying = true;
    }
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
        resetPlayer();
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