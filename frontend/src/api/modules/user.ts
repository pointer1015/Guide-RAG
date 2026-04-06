import request from '../client';
import type { BackendResponse } from '../client';

export interface UpdateProfileReq {
    displayName: string;
}

export interface ChangePasswordReq {
    oldPassword:  string;
    newPassword:  string;
}

export const userApi = {
    /**
     * 修改个人资料
     */
    updateProfile(data: UpdateProfileReq) {
        return request.put<BackendResponse<void>>('/user/profile', data);
    },

    /**
     * 上传头像
     */
    uploadAvatar(file: File) {
        const formData = new FormData();
        formData.append('file', file);
        return request.post<BackendResponse<string>>('/user/avatar', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },

    /**
     * 修改密码
     */
    changePassword(data: ChangePasswordReq) {
        return request.put<BackendResponse<void>>('/user/password', data);
    },
};
