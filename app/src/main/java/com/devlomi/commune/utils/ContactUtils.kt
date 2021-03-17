package com.devlomi.commune.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.devlomi.commune.extensions.observeSingleValueEvent
import com.devlomi.commune.model.ExpandableContact
import com.devlomi.commune.model.PhoneContact
import com.devlomi.commune.model.realms.PhoneNumber
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.network.FireManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.getValue
import com.thoughtbot.expandablecheckrecyclerview.models.MultiCheckExpandableGroup
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.wafflecopter.multicontactpicker.ContactResult
import ezvcard.Ezvcard
import ezvcard.VCard
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import io.realm.RealmList
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Created by Devlomi on 03/08/2017.
 */
object ContactUtils {
    private fun getRawContactsObservable(context: Context): Observable<PhoneContact> {

        return Observable.create { emitter: ObservableEmitter<PhoneContact> ->


            val contactsList: MutableList<PhoneContact> = ArrayList()
            val uri = ContactsContract.Contacts.CONTENT_URI
            val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
            )
            val selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'"
            val selectionArgs: Array<String>? = null
            val sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"

            // Build adapter with contact entries
//            var mCursor: Cursor? = null
            var mPhoneNumCursor: Cursor? = null
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.let { mCursor ->
                try {


                    while (mCursor.moveToNext()) {
                        //get contact name
                        val name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))

                        //get contact name
                        val contactID = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts._ID))
                        //create new phoneContact object
                        val contact = PhoneContact()
                        contact.id = contactID.toInt()
                        contact.name = name


                        //get all phone numbers in this contact if it has multiple numbers
                        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(contactID), null)?.let { phoneNumCursor ->

                            mPhoneNumCursor = phoneNumCursor

                            phoneNumCursor?.moveToFirst()


                            //create empty list to fill it with phone numbers for this contact
                            val phoneNumberList: MutableList<String> = ArrayList()
                            while (!phoneNumCursor.isAfterLast) {
                                //get phone number
                                val number = phoneNumCursor.getString(phoneNumCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))


                                //prevent duplicates numbers
                                if (!phoneNumberList.contains(number)) phoneNumberList.add(number)
                                phoneNumCursor.moveToNext()
                            }

                            //fill contact object with phone numbers
                            contact.phoneNumbers = phoneNumberList
                            //add final phoneContact object to contactList
                            contactsList.add(contact)
                            emitter.onNext(contact)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    mCursor?.close()
                    mPhoneNumCursor?.close()
                    emitter.onComplete()
                }
            }
        }

    }


//    fun getRawContacts(context: Context): List<PhoneContact> {
//        val contactsList: MutableList<PhoneContact> = ArrayList()
//        val uri = ContactsContract.Contacts.CONTENT_URI
//        val projection = arrayOf(
//                ContactsContract.Contacts._ID,
//                ContactsContract.Contacts.DISPLAY_NAME
//        )
//        val selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'"
//        val selectionArgs: Array<String>? = null
//        val sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
//
//        // Build adapter with contact entries
//        var mCursor: Cursor? = null
//        var phoneNumCursor: Cursor? = null
//        try {
//            mCursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
//            while (mCursor.moveToNext()) {
//                //get contact name
//                val name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
//
//                //get contact name
//                val contactID = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts._ID))
//                //create new phoneContact object
//                val contact = PhoneContact()
//                contact.id = contactID.toInt()
//                contact.name = name
//
//
//                //get all phone numbers in this contact if it has multiple numbers
//                phoneNumCursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                        null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(contactID), null)
//                phoneNumCursor.moveToFirst()
//
//
//                //create empty list to fill it with phone numbers for this contact
//                val phoneNumberList: MutableList<String> = ArrayList()
//                while (!phoneNumCursor.isAfterLast) {
//                    //get phone number
//                    val number = phoneNumCursor.getString(phoneNumCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
//
//
//                    //prevent duplicates numbers
//                    if (!phoneNumberList.contains(number)) phoneNumberList.add(number)
//                    phoneNumCursor.moveToNext()
//                }
//
//                //fill contact object with phone numbers
//                contact.phoneNumbers = phoneNumberList
//                //add final phoneContact object to contactList
//                contactsList.add(contact)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        } finally {
//            mCursor?.close()
//            phoneNumCursor?.close()
//        }
//        return contactsList
//    }

    //format number to international number
    //if number is not with international code (+1 for example) we will add it
    //depending on user country ,so if the user number is +1 1234-111-11
    //we will add +1 in this case for all the numbers
    //and if it's contains "-" we will remove them
    private fun formatNumber(countryCode: String, number: String): String? {
        val context = MyApp.context()
        val util = PhoneNumberUtil.createInstance(context)
        val phoneNumber: Phonenumber.PhoneNumber
        var phone: String? = number
        try {
            //format number depending on user's country code
            phoneNumber = util.parse(number, countryCode)
            phone = util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (e: NumberParseException) {
            e.printStackTrace()
        }

        //remove empty spaces and dashes and ()
        if (phone != null) phone = phone
                .replace(" ", "")
                .replace("-", "")
                .replace("\\(", "")
                .replace("\\)", "")
        return phone
    }

    //get the Contact name from phonebook by number
    @JvmStatic
    fun queryForNameByNumber(phone: String): String {
        val context = MyApp.context()
        var name = phone
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            return name
        }
        return name
    }

    //check if a contact is exists in phonebook
    @JvmStatic
    fun contactExists(context: Context, number: String?): Boolean {
        var cur: Cursor? = null

        try {
            val lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val mPhoneNumberProjection = arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME)
            cur = context.contentResolver.query(lookupUri, mPhoneNumberProjection, null, null, null)
            if (cur != null) {
                if (cur.moveToFirst()) {
                    return true
                }
            }
        } catch (e: Exception) {
        } finally {
            cur?.close()
        }

        return false


    }

    @JvmStatic
    fun syncContacts(): Completable {
        val realmHelper = RealmHelper.getInstance()
        return Completable.create { emitter ->

            fetchContacts().subscribeOn(Schedulers.io()).subscribe({ user ->

                val storedUser = realmHelper.getUser(user.uid)
                if (storedUser == null) {
                    realmHelper.saveObjectToRealm(user)
                } else {
                    realmHelper.updateUserInfo(user, storedUser, user.userName, user.isStoredInContacts)
                }

            }, { throwable ->

                emitter.onError(throwable)
            }, {
                //onComplete
                SharedPreferencesManager.setContactSynced(true)
                SharedPreferencesManager.setLastSyncContacts(Date().time)
                emitter.onComplete()
            })
        }
    }

    private fun fetchContacts(): Observable<User> {

        val contactsObservable = getRawContactsObservable(MyApp.context())

        val countryCode = SharedPreferencesManager.getCountryCode()


        return contactsObservable.flatMap { contact ->
            return@flatMap Observable.fromIterable(contact.phoneNumbers).map { Pair(contact, it) }
        }.flatMap { pair ->
            val contact = pair.first
            val number = pair.second
            val formattedNumber = formatNumber(countryCode, number) ?: ""
            return@flatMap Observable.just(formattedNumber).map { Pair(contact, it) }
        }.flatMap {
            val contact = it.first
            val formattedNumber = it.second

            if (FireManager.isHasDeniedFirebaseStrings(formattedNumber)) {
                return@flatMap Observable.empty<Pair<PhoneContact, DataSnapshot>?>()
            }

            return@flatMap FireConstants.uidByPhone.child(formattedNumber).observeSingleValueEvent().toObservable().map { Pair(contact, it) }


        }.map {
            val contact = it.first
            val snapshot = it.second
            val uid = snapshot.value as? String
            return@map Pair(contact, uid)

        }.flatMap { pair ->

            val contact = pair.first
            val uid = pair.second
                    ?: return@flatMap Observable.empty<Pair<PhoneContact, DataSnapshot>?>()

            return@flatMap FireConstants.usersRef.child(uid).observeSingleValueEvent().toObservable().map { Pair(contact, it) }

        }.map {
            val contact = it.first
            val snapshot = it.second

            val user = snapshot.getValue<User>()
            val userName = if (contact.name.isEmpty()) user?.phone else contact.name
            user?.userName = userName

            user?.isStoredInContacts = true
            user?.uid = snapshot.key
            return@map user
        }.flatMap { user ->
            if (user == null) return@flatMap Observable.empty<User?>()

            return@flatMap Observable.just(user)
        }


    }


    //convert vCard (contact) Text to organized vCard object
    //this is used when user shares a contact to our app
    //an it's shared as String vCard (vcf)
    @JvmStatic
    fun getContactAsVcard(context: Context, uri: Uri?): List<VCard> {
        val cr = context.contentResolver
        var stream: InputStream? = null
        try {
            stream = cr.openInputStream(uri!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        val fileContent = StringBuffer("")
        var ch: Int
        try {
            while (stream!!.read().also { ch = it } != -1) fileContent.append(ch.toChar())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val data = String(fileContent)
        return Ezvcard.parse(data).all()
    }


    //convert vCard List to a List of ExpandableContact
    @JvmStatic
    fun getContactNamesFromVcard(vcards: List<VCard>): List<ExpandableContact> {
        val contactNameList: MutableList<ExpandableContact> = ArrayList()
        for (vcard in vcards) {
            //get contact name
            val fullName = vcard.formattedName.value
            //get contact numbers
            val telephoneNumbers = vcard.telephoneNumbers
            //create new List to fill it with phone numbers
            val numberList = RealmList<PhoneNumber>()

            //add numbers to list
            for (telephoneNumber in telephoneNumbers) {
                numberList.add(PhoneNumber(telephoneNumber.text))
            }

            //create new ExpandableContact object
            val contactName = ExpandableContact(fullName, numberList)
            //add contact to final list
            contactNameList.add(contactName)
        }
        return contactNameList
    }

    //convert the contacts that the user's picked into an ExpandableContact list
    @JvmStatic
    fun getContactsFromContactResult(results: List<ContactResult>): List<ExpandableContact> {
        val contactList: MutableList<ExpandableContact> = ArrayList()
        for (result in results) {
            val phoneNumbers = RealmList<PhoneNumber>()
            for (s in result.phoneNumbers) {
                if (!phoneNumbers.contains(PhoneNumber(s.number))) phoneNumbers.add(PhoneNumber(s.number))
            }
            val contactName = ExpandableContact(result.displayName, phoneNumbers)
            contactList.add(contactName)
        }
        return contactList
    }

    //get only selected phone numbers
    @JvmStatic
    fun getContactsFromExpandableGroups(groups: List<ExpandableGroup<*>?>): List<ExpandableContact> {
        val contactNameList: MutableList<ExpandableContact> = ArrayList()
        for (x in groups.indices) {
            val group = groups[x] as MultiCheckExpandableGroup? ?: continue
            val name = group.title
            val phoneNumberList = RealmList<PhoneNumber>()
            for (i in group.items.indices) {
                val phoneNumber = group.items[i] as PhoneNumber
                //get only selected numbers && prevent duplicate numbers
                if (group.selectedChildren[i] && !phoneNumberList.contains(phoneNumber)) {
                    phoneNumberList.add(phoneNumber)
                }
            }
            if (!phoneNumberList.isEmpty()) contactNameList.add(ExpandableContact(name, phoneNumberList))
        }
        return contactNameList
    }

}