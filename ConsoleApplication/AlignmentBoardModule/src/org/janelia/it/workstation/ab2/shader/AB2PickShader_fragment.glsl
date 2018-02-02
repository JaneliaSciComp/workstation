#version 410

flat in int fragmentPickId;

layout (location=0) out int fragmentPickId2;

void main() {
    fragmentPickId2=fragmentPickId;
}

