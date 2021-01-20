# Testing the Data wallet app

## Administrative interfaces

1. Test Center - https://demo-aries-agent-admin.igrant.io
2. Travel Company - https://demo-aries-agent-admin2.igrant.io

## Establishing connection to an organization

1. Scan the QR code for the organization (ideally shown in the customer portal)

Note: Establishing connection, in real world would be equivalent to onboarding individual to organization as a customer or linking existing customer.

### Fetch the connection ID

#### Method 1

1. Go to this link (Choose the link based on which organization you are trying to fetch the connection ID)
   1. Test center - https://demo-aries-agent-admin.igrant.io/api/doc#/connection/get_connections
   2. Travel company - https://demo-aries-agent-admin2.igrant.io/api/doc#/connection/get_connections
2. Change the connection state field to "active" in the parameters
3. Click execute.
4. Scroll down the response, to the end, last entry should correspond to your established connection.
5. Check their_label in the connection response to see if it matches your device name.
6. Copy the connection_id

#### Method 2

1. Open the "Data wallet" mobile app, Click on "Add data"
2. After establishing connection in the mobile app, the corresponding organization will be shown in the "Choose organization" page.
3. Long press on the organization logo for 8 seconds
4. A DID corresponding to organization would be copied to your clipboard
5. Go to this link (Choose the link based on which organization you are trying to fetch the connection ID)
   1. Test center - https://demo-aries-agent-admin.igrant.io/api/doc#/connection/get_connections
   2. Travel company - https://demo-aries-agent-admin2.igrant.io/api/doc#/connection/get_connections
6. Change the connection state field to "active" in the parameters
7. Update their_did field with this copied DID
8. Click execute.
9. Response should contain entry corresponding to your established connection.
10. Check their_label in the connection response to see if it matches your device name.
11. Copy the connection_id

Note: This would be an automated flow integrated to existing organization IT systems. Organizations should map, connection ID to internal customer ID.

## Test center offering data to the individual

### Organization side flow

1. Go to this link, https://demo-aries-agent-admin.igrant.io/api/doc#/issue-credential/post_issue_credential_send_offer
2. Update the  body with below JSON,

```json
{
  "comment": "Covid19 Test Result",
  "auto_remove": false,
  "trace": false,
  "cred_def_id": "TAzgX7huQhYqHduEjoBbYr:3:CL:342:default",
  "connection_id": "<connection_id>",
  "credential_preview": {
    "@type": "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/issue-credential/1.0/credential-preview",
    "attributes": [
      {
        "name": "Patient name",
        "value": "George J Padayatti"
      },
      {
        "name": "Patient age",
        "value": "24"
      },
      {
        "name": "Test date",
        "value": "2020-11-21T07:03:12Z"
      },
      {
        "name": "Test result",
        "value": "-ve"
      },
      {
        "name": "Test result ID",
        "value": "a12e81a0-380c-44c8-88ba-52cc46f784e2"
      }
    ]
  },
  "auto_issue": true
}
```

3. Replace "<connection_id>" in the above JSON with the corresponding connection_id copied in the earlier sections
4. Click execute.

### Individual side flow

1. Individual should navigate in the mobile app to see the offer
   1. Click "Add data"
   2. Select corresponding organization
   3. Click on the Data model with "+" button activated
   4. Click on the offer to view details
   5. Accept the offer

## Individual exchanges data towards Travel company

Example scenario: Individual visits a restaurant, the door will only open, if the individual is Covid19 negative.

### Data exchange through QR code (Driven by the user)

1. In the mobile app, click "Exchange data"
2. Click the "+" button at the bottom
3. Scan the QR code

### Data exchange without QR code (Initiated by organization)

Example scenario: Organization interacts with the existing customer to fetch some required data to complete a process.

#### Organization side flow

1. Go to this link, https://demo-aries-agent-admin2.igrant.io/api/doc#/present-proof/post_present_proof_send_request
2. Update the body with below JSON,

```
{
  "connection_id": "<connection_id>",
  "comment": "For safety reasons, we verify customers for COVID-19, before permitting them to travel.",
  "trace": false,
  "proof_request": {
    "nonce": "1234567890",
    "name": "COVID-19 test verification.",
    "version": "1.0",
    "requested_attributes": {
      "additionalProp1": {
        "name": "Test date",
        "restrictions": []
      },
      "additionalProp2": {
        "name": "Patient age",
        "restrictions": []
      },
      "additionalProp3": {
        "name": "Patient name",
        "restrictions": []
      }
    },
    "requested_predicates": {}
  }
}
```

3. Replace "<connection_id>" in the above JSON with the corresponding connection_id copied in the earlier sections
4. Click execute.

#### Individual side flow

1. Individual should navigate in the mobile app to see the data exchange request
   1. Click "Exchange data"
   2. Select corresponding data exchange request to see the preview of data being requested and exchanged.
   3. Click on "Exchange"
