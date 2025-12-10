import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../auth/services/auth.service';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: string;
  avatar?: string;
}

export interface UpdateUserRequest {
  name?: string;
  password?: string;
  avatar?: string | null;
}

export interface UpdateUserWithFileRequest {
  name?: string;
  password?: string;
  avatarFile?: File;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/api/users`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getUserById(id: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${id}/custom`);
  }

  getCurrentUserId(): string | null {
     // On recupere l'id du current user from the local storage
    let userString = localStorage.getItem('currentUser');
    if (!userString) {
      return null
    }

    const user = JSON.parse(userString);
    if (!user || !user.id) {
      return null
    }
    return user.id;
  }

  /**
   * Get current user profile from backend
   */
  getCurrentUserProfile(): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    });

    console.log('Fetching user profile for user ID : ', this.getCurrentUserId());

    // Assuming the backend has an endpoint to get current user info
    return this.http.get<any>(`${this.apiUrl}/${this.getCurrentUserId()}/custom`, { headers })
      .pipe(catchError(this.handleError));
  }

  /**
   * Get user profile by ID
   */
  getUserProfile(userId: string): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    });

    return this.http.get<any>(`${this.apiUrl}/${userId}/custom`, { headers })
      .pipe(catchError(this.handleError));
  }

  /**
   * Update user profile (text fields only) - PATCH instead of PUT to update only modified fields
   */
  updateUserProfile(userData: UpdateUserRequest): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    // Create object with only the fields that have values (exclude undefined, but allow null)
    const updateData: any = {};
    if (userData.name !== undefined) {
      updateData.name = userData.name;
    }
    if (userData.password !== undefined) {
      updateData.password = userData.password;
    }
    if (userData.avatar !== undefined) {
      updateData.avatar = userData.avatar; // This can be null to remove avatar
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    });

    // Use PATCH instead of PUT to update only the provided fields
    return this.http.patch<any>(`${this.apiUrl}/${this.getCurrentUserId()}`, updateData, { headers })
      .pipe(catchError(this.handleError));
  }

  /**
   * Update user profile with file upload (including avatar)
   * First uploads the image to /api/media, then updates user with the returned URL
   */
  updateUserProfileWithFile(userData: UpdateUserWithFileRequest): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    if (!userData.avatarFile) {
      // No file to upload, just update text fields
      return this.updateUserProfile({
        name: userData.name,
        password: userData.password
      });
    }

    // First, upload the image to /api/media
    const formData = new FormData();
    formData.set('file', userData.avatarFile, userData.avatarFile.name);

    console.log('Uploading avatar file to /api/media');

    return this.http.post<any>(`${environment.apiUrl}/api/media`, formData).pipe(
      switchMap((mediaResponse) => {
        console.log('Media upload response:', mediaResponse);
        
        // Now update the user profile with the image URL and other data
        const updateData: UpdateUserRequest = {};
        
        if (userData.name !== undefined && userData.name !== null) {
          updateData.name = userData.name;
        }
        if (userData.password !== undefined && userData.password !== null) {
          updateData.password = userData.password;
        }
        
        // Set the avatar URL from the media response
        updateData.avatar = mediaResponse?.imageUrl;

        return this.updateUserProfile(updateData);
      }),
      catchError((error) => {
        console.error('Error in updateUserProfileWithFile:', error);
        return this.handleError(error);
      })
    );
  }

  /**
   * Upload avatar only - uploads to /api/media first, then updates user avatar
   */
  uploadAvatar(avatarFile: File): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    // First, upload the image to /api/media
    const formData = new FormData();
    formData.set('file', avatarFile, avatarFile.name);

    console.log('Uploading avatar file to /api/media');

    return this.http.post<any>(`${environment.apiUrl}/api/media`, formData).pipe(
      switchMap((mediaResponse) => {
        console.log('Media upload response:', mediaResponse);
        
        // Update user profile with the new avatar URL
        return this.updateUserProfile({
          avatar: mediaResponse?.imageUrl
        });
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Delete user avatar - sets avatar to null
   */
  deleteAvatar(): Observable<UserProfile> {
    const token = this.authService.getToken();
    
    if (!token) {
      return throwError(() => new Error('No authentication token available'));
    }

    // Update user profile to remove avatar (set to null)
    return this.updateUserProfile({
      avatar: null
    });
  }

  private handleError = (error: HttpErrorResponse) => {
    console.error('UserService HTTP Error:', error);

    let errorMessage = 'An unknown error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Client Error: ${error.error.message}`;
    } else {
      // Server-side error
      switch (error.status) {
        case 400:
          errorMessage = 'Bad request. Please check your input.';
          break;
        case 401:
          errorMessage = 'Authentication failed. Please log in again.';
          break;
        case 403:
          errorMessage = 'Access forbidden. You do not have permission.';
          break;
        case 404:
          errorMessage = 'User not found.';
          break;
        case 409:
          errorMessage = 'Email already exists. Please use a different email.';
          break;
        case 422:
          errorMessage = 'Invalid data provided. Please check your input.';
          break;
        case 500:
          errorMessage = 'Server error. Please try again later.';
          break;
        case 0:
          errorMessage = 'Network error. Please check your connection.';
          break;
        default:
          errorMessage = `Server returned code ${error.status}: ${error.error?.message || error.message}`;
      }
    }

    return throwError(() => new Error(errorMessage));
  };
}
