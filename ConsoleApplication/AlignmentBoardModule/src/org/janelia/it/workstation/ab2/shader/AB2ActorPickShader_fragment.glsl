#version 410

flat in int fragmentPickId;

layout (location=1) out int fragmentPickId2;

void main() {
    //fragmentPickId2=fragmentPickId;
    fragmentPickId2=7;
}

