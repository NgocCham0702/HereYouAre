package com.cham.appvitri.viewModel

import androidx.lifecycle.ViewModel
import com.cham.appvitri.model.FriendRequestModel
import com.cham.appvitri.model.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<UserModel>>(emptyList())
    val users = _users.asStateFlow()

    private val _requests = MutableStateFlow<List<FriendRequestModel>>(emptyList())
    val requests = _requests.asStateFlow()

    init {
        loadUsers()
        loadRequests()
    }

    private fun loadUsers() {
        db.collection("users").addSnapshotListener { snap, _ ->
            if (snap != null) {
                _users.value = snap.documents.mapNotNull { doc ->
                    doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                }
            }
        }
    }


    private fun loadRequests() {
        db.collection("friend_requests").addSnapshotListener { snap, _ ->
            if (snap != null) {
                _requests.value = snap.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequestModel::class.java)?.copy(requestId = doc.id)
                }
            }
        }
    }


    fun deleteUser(uid: String) {
        db.collection("users").document(uid).delete()
    }

    fun deleteRequest(id: String) {
        db.collection("friend_requests").document(id).delete()
    }
}
