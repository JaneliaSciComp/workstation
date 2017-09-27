#version 410

flat in int fragmentPickId3;

out int fragmentPickId2;

void main() {
    fragmentPickId2=fragmentPickId3;
}

