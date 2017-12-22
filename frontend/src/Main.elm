port module Main exposing (..)

import Html exposing (Html, text, div, h1, img, button)
import Html.Attributes exposing (src, class, attribute, id)

port renderGoogleSSOButton : String -> Cmd msg
port username : (String -> msg) -> Sub msg

---- MODEL ----

type alias Model =
    {username: String}


init : (Model, Cmd Msg)
init =
    (Model "", renderGoogleSSOButton "g-sso-button")


---- UPDATE ----


type Msg
    = NoOp | InitGoogleSSO


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    ( model, Cmd.none )


---- VIEW ----


view : Model -> Html Msg
view model =
    div []
        [ img [ src "/logo.svg" ] []
        , h1 [] [ text "Your Elm App is working!" ]
        , div [id "g-sso-button"] [text "click me"]
          div [id "g-sso-button-2", class ] [text "click me"]
        ]


---- SUBSCRIPTIONS ----


subscriptions : Model -> Sub Msg
subscriptions model =
    username




---- PROGRAM ----

main : Program Never Model Msg
main =
    Html.program
        { view = view
        , init = init
        , update = update
        , subscriptions = subscriptions
        }
