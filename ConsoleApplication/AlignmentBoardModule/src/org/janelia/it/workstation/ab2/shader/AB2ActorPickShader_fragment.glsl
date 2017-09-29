#version 410

//flat in int fragmentPickId;

//out int fragmentPickId2;
layout (location=0) out vec4 fColor;

void main() {
    //fragmentPickId2=fragmentPickId;
    //fragmentPickId2=7;
    fColor=vec4(0.3, 0.7, 0.2, 1.0);
}

