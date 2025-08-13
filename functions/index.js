const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendTaskNotification = functions.database
    .ref("/notifications/{notificationId}")
    .onCreate(async (snapshot, context) => {
      const notification = snapshot.val();

      // 1. Get the admin's FCM token
      const adminTokenSnapshot = await admin.database()
          .ref(`user_tokens/${notification.adminId}`)
          .once("value");

      const adminToken = adminTokenSnapshot.val();
      if (!adminToken) {
        functions.logger.log("Admin FCM token not found.");
        return null;
      }

      // 2. Prepare the notification payload
      const payload = {
        notification: {
          title: `Task ${notification.newStatus}: ${notification.taskTitle}`,
          body: `By ${notification.employeeName}`,
          sound: "default",
        },
        data: {
          taskId: context.params.notificationId,
          type: "TASK_UPDATE",
        },
      };

      // 3. Send the notification
      return admin.messaging().sendToDevice(adminToken, payload)
          .then((response) => {
            functions.logger.log("Successfully sent FCM:", response);
            return null;
          })
          .catch((error) => {
            functions.logger.error("FCM error:", error);
            return null;
          });
    });
