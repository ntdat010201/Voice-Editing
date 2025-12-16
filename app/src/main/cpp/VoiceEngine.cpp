#include "VoiceEngine.h"
#include <cstring>

void VoiceEngine::pushPcm(const float* data, int frames) {
    pcmBuffer_.push(data, frames);
}

void VoiceEngine::clearBuffer() {
    pcmBuffer_.clear();
}

bool VoiceEngine::start() {
    return true;
}

void VoiceEngine::stop() {
}

void VoiceEngine::setPitch(float pitch) {
    pitch_.store(pitch);
}
